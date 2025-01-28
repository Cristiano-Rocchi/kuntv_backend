package kun.kuntv_backend.services;

import kun.kuntv_backend.entities.Film;
import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.payloads.NewVideoDTO;
import kun.kuntv_backend.payloads.VideoRespDTO;
import kun.kuntv_backend.repositories.VideoRepository;
import kun.kuntv_backend.repositories.StagioneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.*;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private static final Logger LOGGER = Logger.getLogger(VideoService.class.getName());

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private StagioneRepository stagioneRepository;

    @Autowired
    private S3Client s3Client; // Iniettato dal configuratore

    @Value("${backblaze.b2.keyId}")
    private String keyId;

    @Value("${backblaze.b2.applicationKey}")
    private String applicationKey;

    @Value("${backblaze.b2.bucketName}")
    private String b2BucketName; // Nome del bucket Backblaze B2

    public List<VideoRespDTO> getAllVideos() {
        List<Video> videos = videoRepository.findAll();

        return videos.stream()
                .map(video -> {
                    LOGGER.info("🔹 Generazione link firmato per video ID: " + video.getId() + " - URL salvato: " + video.getFileLink());
                    return new VideoRespDTO(
                            video.getId(),
                            video.getTitolo(),
                            video.getDurata(),
                            generatePresignedUrl(video.getFileLink()), // Genera URL firmato
                            video.getStagione() != null ? video.getStagione().getTitolo() : null,
                            video.getSezione().getTitolo()
                    );
                })
                .collect(Collectors.toList());
    }

    public Video getVideoById(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ID del video non valido: " + id));

        LOGGER.info("🔹 Generazione link firmato per video ID: " + video.getId() + " - URL salvato: " + video.getFileLink());
        video.setFileLink(generatePresignedUrl(video.getFileLink()));

        return video;
    }


    public Video updateVideo(UUID id, Video updatedVideo) {
        Video existingVideo = videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Video non trovato con ID: " + id));

        if (updatedVideo.getTitolo() != null) {
            existingVideo.setTitolo(updatedVideo.getTitolo());
        }
        if (updatedVideo.getDurata() != null) {
            existingVideo.setDurata(updatedVideo.getDurata());
        }

        try {
            return videoRepository.save(existingVideo);
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante l'aggiornamento del video.");
        }
    }

    public boolean deleteVideo(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Video non trovato con ID: " + id));

        try {
            // Cancella il file su Backblaze B2
            String fileName = video.getFileLink().substring(video.getFileLink().lastIndexOf("/") + 1);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(b2BucketName)
                    .key(fileName)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);

            LOGGER.info("File cancellato da Backblaze B2: " + fileName);

            videoRepository.delete(video);
            return true;
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante la cancellazione del video.");
        }
    }



    public Video createVideo(NewVideoDTO dto, MultipartFile file) {
        Stagione stagione = stagioneRepository.findById(dto.getStagioneId())
                .orElseThrow(() -> new NotFoundException("Stagione non trovata con l'ID: " + dto.getStagioneId()));

        Sezione sezione = stagione.getSezione();

        Video video = new Video();
        video.setTitolo(dto.getTitolo());
        video.setDurata(dto.getDurata());
        video.setStagione(stagione);
        video.setSezione(sezione);

        File tempFile = null;
        File compressedFile = null;

        try {
            tempFile = File.createTempFile("upload_", file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }

            LOGGER.info("File originale ricevuto: " + tempFile.getAbsolutePath() + " - Dimensione: " + tempFile.length() / 1024 + " KB");

            // 🔹 Comprimi il video con FFmpeg prima di caricarlo
            compressedFile = compressVideo(tempFile);

            // 🔹 Caricare il video su Backblaze B2
            String uploadedUrl = uploadToBackblazeB2(compressedFile);
            if (uploadedUrl != null) {
                LOGGER.info("🔹 Video caricato su Backblaze B2 con successo: " + uploadedUrl);

                // 🔹 Genera il link firmato
                String presignedUrl = generatePresignedUrl(uploadedUrl);
                LOGGER.info("✅ URL firmato generato con successo: " + presignedUrl);

                // 🔹 Imposta l'URL firmato nel video
                video.setFileLink(presignedUrl);

                return videoRepository.save(video);
            }

        } catch (Exception e) {
            LOGGER.severe("❌ Errore nella gestione del file: " + e.getMessage());
            throw new InternalServerErrorException("Errore nella gestione del file: " + e.getMessage());
        } finally {
            if (tempFile != null) tempFile.delete();
            if (compressedFile != null) compressedFile.delete();
        }

        return video;
    }

    private File compressVideo(File inputFile) throws IOException, InterruptedException {
        File compressedFile = new File(inputFile.getParent(), "compressed_" + inputFile.getName());

        String command = String.format("C:\\ffmpeg-7.1-full_build\\bin\\ffmpeg.exe -i \"%s\" -vcodec libx265 -crf 24 \"%s\"",
                inputFile.getAbsolutePath(),
                compressedFile.getAbsolutePath());

        long startTime = System.currentTimeMillis();
        Process process = Runtime.getRuntime().exec(command);

        // Leggere l'output di FFmpeg per evitare blocchi
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info("FFmpeg: " + line);
            }
        }

        int exitCode = process.waitFor();
        long endTime = System.currentTimeMillis();

        if (exitCode != 0 || !compressedFile.exists() || compressedFile.length() == 0) {
            LOGGER.warning("Compressione fallita (Exit Code: " + exitCode + "). Si userà il file originale.");
            return inputFile; // Se la compressione fallisce, restituisci il file originale
        } else {
            LOGGER.info("Compressione completata: " + compressedFile.getAbsolutePath() +
                    " - Dimensione: " + compressedFile.length() / 1024 + " KB - Tempo impiegato: " + (endTime - startTime) + " ms");
            return compressedFile; // Restituisci il file compresso
        }
    }

    private String uploadToBackblazeB2(File file) throws IOException {
        PutObjectRequest uploadRequest = PutObjectRequest.builder()
                .bucket(b2BucketName)
                .key(file.getName())
                .build();

        PutObjectResponse response = s3Client.putObject(uploadRequest, RequestBody.fromFile(file));

        return s3Client.utilities().getUrl(builder -> builder.bucket(b2BucketName).key(file.getName())).toString();
    }

    private String generatePresignedUrl(String fileUrl) {
        LOGGER.info("🔹 Entrato in generatePresignedUrl per: " + fileUrl);

        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create("https://s3.us-east-005.backblazeb2.com"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(keyId, applicationKey)))
                .build()) {

            LOGGER.info("🔹 Configurato S3Presigner con successo!");

            // ✅ Estrai solo il nome del file, rimuovendo l'URL completo
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            LOGGER.info("🔹 Nome del file estratto: " + fileName);

            // 🔹 LOG: Stampa il bucket
            LOGGER.info("🔹 Nome del bucket utilizzato: " + b2BucketName);

            // 🔹 Creazione della richiesta
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(b2BucketName)
                    .key(fileName)
                    .build();

            LOGGER.info("🔹 Creata richiesta per il file: " + fileName);

            // 🔹 Generazione del link firmato
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(r ->
                    r.signatureDuration(Duration.ofHours(1))
                            .getObjectRequest(getObjectRequest));

            String signedUrl = presignedRequest.url().toString();
            LOGGER.info("✅ URL firmato generato con successo: " + signedUrl);

            return signedUrl;
        } catch (Exception e) {
            LOGGER.severe("❌ Errore nella generazione del link firmato: " + e.getMessage());
            throw new InternalServerErrorException("Errore nella generazione del link firmato.");
        }
    }






}
