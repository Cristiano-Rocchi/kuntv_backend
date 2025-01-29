package kun.kuntv_backend.services;

import kun.kuntv_backend.config.BackblazeB2Config;
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

    private static final Logger LOGGER = Logger.getLogger(kun.kuntv_backend.services.VideoService.class.getName());

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private StagioneRepository stagioneRepository;

    @Autowired
    private BackblazeB2Config backblazeB2Config;

    @Autowired
    private Map<String, S3Client> backblazeAccounts;

    @Autowired
    private Map<String, String> keyIdMapping;
    public List<VideoRespDTO> getAllVideos() {
        List<Video> videos = videoRepository.findAll();

        return videos.stream()
                .map(video -> {
                    LOGGER.info("🔹 Recupero video ID: " + video.getId());
                    return new VideoRespDTO(
                            video.getId(),
                            video.getTitolo(),
                            video.getDurata(),
                            generatePresignedUrl(video.getFileLink()),
                            video.getStagione() != null ? video.getStagione().getTitolo() : null,
                            video.getSezione().getTitolo()
                    );
                })
                .collect(Collectors.toList());
    }

    public Video getVideoById(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ID del video non valido: " + id));

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

        return videoRepository.save(existingVideo);
    }

    public boolean deleteVideo(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Video non trovato con ID: " + id));

        try {
            // Estrai il nome del file e il bucket associato
            String fileName = video.getFileLink().substring(video.getFileLink().lastIndexOf("/") + 1);
            String bucketName = video.getSezione().getTitolo();

            // Recupera il client S3 associato al bucket
            S3Client s3Client = backblazeAccounts.get(bucketName);
            if (s3Client == null) {
                throw new InternalServerErrorException("❌ Nessun account Backblaze trovato per il bucket: " + bucketName);
            }

            // Esegui la cancellazione dal bucket
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);

            LOGGER.info("✅ File eliminato con successo da Backblaze B2: " + fileName);

            videoRepository.delete(video);
            return true;
        } catch (Exception e) {
            throw new InternalServerErrorException("❌ Errore durante la cancellazione del video: " + e.getMessage());
        }
    }

    public Video createVideo(NewVideoDTO dto, MultipartFile file) {
        Stagione stagione = stagioneRepository.findById(dto.getStagioneId())
                .orElseThrow(() -> new NotFoundException("Stagione non trovata con l'ID: " + dto.getStagioneId()));

        Sezione sezione = stagione.getSezione();

        // 📦 🔍 Determina il bucket corretto
        String bucketName = determineBucket();
        LOGGER.info("📦 Bucket selezionato per il video: " + bucketName);

        // 🔍 Recupera il client S3 associato al bucket
        S3Client s3Client = backblazeAccounts.get(bucketName);
        if (s3Client == null) {
            throw new InternalServerErrorException("❌ Nessun client S3 trovato per il bucket: " + bucketName);
        }

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

            LOGGER.info("📁 File originale ricevuto: " + tempFile.getAbsolutePath());

            // 🔹 Comprimi il video con FFmpeg prima di caricarlo
            compressedFile = compressVideo(tempFile);

            LOGGER.info("🚀 Inizio caricamento su Backblaze B2...");
            String uploadedUrl = uploadToBackblazeB2(s3Client, bucketName, compressedFile);
            LOGGER.info("✅ Caricamento completato, URL: " + uploadedUrl);

            video.setFileLink(uploadedUrl);
            return videoRepository.save(video);
        } catch (Exception e) {
            LOGGER.severe("❌ Errore durante la creazione del video: " + e.getMessage());
            throw new InternalServerErrorException("Errore nella gestione del file: " + e.getMessage());
        } finally {
            if (tempFile != null) tempFile.delete();
            if (compressedFile != null) compressedFile.delete();
        }
    }


    /**
     * 🔹 Determina il bucket corretto da usare
     */
    private String determineBucket() {
        List<String> buckets = new ArrayList<>(keyIdMapping.keySet());

        if (buckets.isEmpty()) {
            throw new InternalServerErrorException("❌ Nessun bucket disponibile per l'upload!");
        }

        // 🔹 Controlla quale bucket ha spazio disponibile
        for (String bucket : buckets) {
            if (hasAvailableSpace(bucket)) {
                LOGGER.info("✅ Bucket assegnato: " + bucket);
                return bucket;
            }
        }

        throw new InternalServerErrorException("❌ Nessun bucket ha spazio disponibile!");
    }

    /**
     * 🔹 Verifica se un bucket ha spazio disponibile
     */
    private boolean hasAvailableSpace(String bucketName) {
        S3Client s3Client = backblazeAccounts.get(bucketName);
        if (s3Client == null) return false;

        try {
            // Esegue una richiesta HeadBucket per verificare se il bucket risponde
            s3Client.headBucket(builder -> builder.bucket(bucketName));
            return true; // Se la richiesta ha successo, il bucket è disponibile
        } catch (Exception e) {
            LOGGER.warning("⚠ Il bucket " + bucketName + " non è disponibile.");
            return false;
        }
    }







    private String uploadToBackblazeB2(S3Client s3Client, String bucketName, File file) throws IOException {
        LOGGER.info("🚀 Inizio caricamento file: " + file.getName() + " su Backblaze B2 (Bucket: " + bucketName + ")...");

        PutObjectRequest uploadRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(file.getName())
                .build();

        s3Client.putObject(uploadRequest, RequestBody.fromFile(file));

        String fileUrl = s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(file.getName())).toString();

        LOGGER.info("✅ Upload completato con successo! URL: " + fileUrl);

        return fileUrl;
    }

    private String generatePresignedUrl(String fileUrl) {
        return fileUrl; // Gestione pre-signed URL da implementare se necessario
    }

    private File compressVideo(File inputFile) throws IOException, InterruptedException {
        File compressedFile = new File(inputFile.getParent(), "compressed_" + inputFile.getName());

        LOGGER.info("🔄 Inizio compressione video con FFmpeg: " + inputFile.getAbsolutePath());

        String command = String.format("ffmpeg -i \"%s\" -vcodec libx265 -crf 24 \"%s\"",
                inputFile.getAbsolutePath(),
                compressedFile.getAbsolutePath());

        Process process = Runtime.getRuntime().exec(command);

        // Leggi l'output di FFmpeg in tempo reale
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info("FFmpeg: " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            LOGGER.warning("⚠ Compressione fallita con codice di uscita: " + exitCode);
            return inputFile; // Se fallisce, carica il file originale
        }

        LOGGER.info("✅ Compressione completata: " + compressedFile.getAbsolutePath());

        return compressedFile;
    }

}