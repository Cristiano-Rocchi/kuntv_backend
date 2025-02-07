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
import software.amazon.awssdk.services.s3.model.*;
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

    //TROVA TUTTI I VIDEO
    public List<VideoRespDTO> getAllVideos() {
        return videoRepository.findAll().stream()
                .map(video -> {
                    // Estrai il bucket dal fileLink
                    String bucketName = extractBucketNameFromUrl(video.getFileLink());

                    // Ottieni le credenziali per il bucket corretto
                    String keyId = backblazeB2Config.keyIdMapping().get(bucketName);
                    String applicationKey = backblazeB2Config.applicationKeyMapping().get(bucketName);

                    if (keyId == null || applicationKey == null) {
                        throw new InternalServerErrorException("❌ Credenziali Backblaze mancanti per il bucket: " + bucketName);
                    }

                    return new VideoRespDTO(
                            video.getId(),
                            video.getTitolo(),
                            video.getDurata(),
                            generatePresignedUrl(video.getFileLink(), bucketName, keyId, applicationKey),
                            video.getStagione() != null ? video.getStagione().getTitolo() : null,
                            video.getSezione().getTitolo(),
                            bucketName,
                            video.getDataCaricamento()
                    );
                })
                .collect(Collectors.toList());
    }



    // TROVA SINGOLO VIDEO
    public VideoRespDTO getVideoById(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ID del video non valido: " + id));

        // Estrai il bucket dal fileLink
        String bucketName = extractBucketNameFromUrl(video.getFileLink());

        // Ottieni le credenziali per il bucket corretto
        String keyId = backblazeB2Config.keyIdMapping().get(bucketName);
        String applicationKey = backblazeB2Config.applicationKeyMapping().get(bucketName);

        if (keyId == null || applicationKey == null) {
            throw new InternalServerErrorException("❌ Credenziali Backblaze mancanti per il bucket: " + bucketName);
        }

        // Genera il link firmato
        String presignedUrl = generatePresignedUrl(video.getFileLink(), bucketName, keyId, applicationKey);

        return new VideoRespDTO(
                video.getId(),
                video.getTitolo(),
                video.getDurata(),
                presignedUrl,
                video.getStagione() != null ? video.getStagione().getTitolo() : null,
                video.getSezione().getTitolo(),
                bucketName,
                video.getDataCaricamento() // 🔹 Aggiunto dataCaricamento
        );
    }


    //MODIFICA VIDEO
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

    //ELIMINA VIDEO
    public boolean deleteVideo(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Video non trovato con ID: " + id));

        try {
            String fileName = video.getFileLink().substring(video.getFileLink().lastIndexOf("/") + 1);

            // Usa la stessa funzione di estrazione del bucketName da fileLink
            String bucketName = extractBucketNameFromUrl(video.getFileLink());

            S3Client s3Client = backblazeAccounts.get(bucketName);
            if (s3Client == null) {
                throw new InternalServerErrorException("❌ Nessun account Backblaze trovato per il bucket: " + bucketName);
            }

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



    // ESTRAI NOME BUCKET DALL'URL
    public String extractBucketNameFromUrl(String fileUrl) {
        try {
            URI uri = new URI(fileUrl);
            String host = uri.getHost(); // Es. "kun-tv2.s3.us-east-005.backblazeb2.com"

            if (host == null || !host.contains(".")) {
                throw new InternalServerErrorException("❌ URL non valido: " + fileUrl);
            }

            // Il bucket è la prima parte del dominio (es. "kun-tv2" da "kun-tv2.s3.us-east-005.backblazeb2.com")
            String bucketName = host.split("\\.")[0];

            LOGGER.info("🔹 Bucket estratto dall'URL: " + bucketName);
            return bucketName;
        } catch (Exception e) {
            throw new InternalServerErrorException("❌ Errore nell'estrazione del bucket dall'URL: " + fileUrl + " - " + e.getMessage());
        }
    }


    //CREAZIONE VIDEO
    public VideoRespDTO createVideo(NewVideoDTO dto, MultipartFile file) {
         /*
    1.Comprimere video
    2.Determinare bucket
    3.Carica file nel bucket
    4.Ottieni le credenziali
    5.Genera Link Firmato
    6.Crea e RestituisceDTO
     */
        Stagione stagione = stagioneRepository.findById(dto.getStagioneId())
                .orElseThrow(() -> new NotFoundException("Stagione non trovata con l'ID: " + dto.getStagioneId()));

        Sezione sezione = stagione.getSezione();

        File tempFile = null;
        File compressedFile = null;

        try {
            tempFile = File.createTempFile("upload_", file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }

            //1.Comprimi il video prima di determinare il bucket
            compressedFile = compressVideo(tempFile);
            long fileSize = compressedFile.length(); // Otteniamo la dimensione del file compresso

            //2.Determina il bucket con spazio sufficiente
            String bucketName = determineBucket(sezione.getTitolo(), fileSize);
            LOGGER.info("📦 Bucket selezionato per il video: " + bucketName);

            S3Client s3Client = backblazeAccounts.get(bucketName);
            if (s3Client == null) {
                throw new InternalServerErrorException("❌ Nessun client S3 trovato per il bucket: " + bucketName);
            }

            Video video = new Video();
            video.setTitolo(dto.getTitolo());
            video.setDurata(dto.getDurata());
            video.setStagione(stagione);
            video.setSezione(sezione);

            //3. Carica il file nel bucket selezionato
            String uploadedUrl = uploadToBackblazeB2(s3Client, bucketName, compressedFile);
            LOGGER.info("✅ File caricato su Backblaze B2: " + uploadedUrl);

            //4. Ottieni le credenziali per generare il presigned URL
            String keyId = backblazeB2Config.keyIdMapping().get(bucketName);
            String applicationKey = backblazeB2Config.applicationKeyMapping().get(bucketName);

            if (keyId == null || applicationKey == null) {
                throw new InternalServerErrorException("❌ Credenziali Backblaze mancanti per il bucket: " + bucketName);
            }

            //5. Genera il link firmato
            String presignedUrl = generatePresignedUrl(uploadedUrl, bucketName, keyId, applicationKey);
            LOGGER.info("✅ Link firmato generato: " + presignedUrl);

            video.setFileLink(uploadedUrl);
            Video savedVideo = videoRepository.save(video);

            //6. Crea e restituisce il DTO con il link firmato
            return new VideoRespDTO(
                    savedVideo.getId(),
                    savedVideo.getTitolo(),
                    savedVideo.getDurata(),
                    presignedUrl,
                    savedVideo.getStagione() != null ? savedVideo.getStagione().getTitolo() : null,
                    savedVideo.getSezione().getTitolo(),
                    bucketName,
                    savedVideo.getDataCaricamento()
            );
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore nella gestione del file: " + e.getMessage());
        } finally {
            if (tempFile != null) tempFile.delete();
            if (compressedFile != null) compressedFile.delete();
        }
    }


    // COMPRIME VIDEO CON FFMPEG
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


    // DETERMINA BUCKET CORRETTO DA USARE
    private String determineBucket(String sezioneNome, long fileSize) {
        List<String> buckets = new ArrayList<>(keyIdMapping.keySet());

        if (buckets.isEmpty()) {
            throw new InternalServerErrorException("❌ Nessun bucket disponibile per l'upload!");
        }

        for (String bucket : buckets) {
            if (hasAvailableSpace(bucket, fileSize)) {
                LOGGER.info("✅ Bucket selezionato con spazio disponibile: " + bucket);
                return bucket;
            }
        }

        throw new InternalServerErrorException("❌ Nessun bucket ha spazio sufficiente per il file di " + fileSize + " byte.");
    }


    // VERIFICA SPAZIO DISPONIBILE NEL BUCKET
    private boolean hasAvailableSpace(String bucketName, long fileSize) {
        S3Client s3Client = backblazeAccounts.get(bucketName);
        if (s3Client == null) {
            LOGGER.warning("⚠ Nessun client S3 trovato per il bucket: " + bucketName);
            return false;
        }

        try {
            // Ottiene i metadati del bucket (Backblaze B2 non fornisce direttamente lo spazio disponibile,

            long usedStorage = getUsedStorage(bucketName);

            // Imposta una soglia massima per il bucket (es. 1 TB = 1_000_000_000_000 bytes)
            long maxBucketSize = 1_000_000_000_000L;

            // Calcola lo spazio disponibile
            long availableSpace = maxBucketSize - usedStorage;

            if (availableSpace >= fileSize) {
                LOGGER.info("✅ Spazio sufficiente nel bucket " + bucketName + ": " + availableSpace + " bytes disponibili.");
                return true;
            } else {
                LOGGER.warning("⚠ Spazio insufficiente nel bucket " + bucketName + ": " + availableSpace + " bytes disponibili, ma il file richiede " + fileSize + " bytes.");
                return false;
            }
        } catch (Exception e) {
            LOGGER.warning("⚠ Errore nel controllare lo spazio disponibile per il bucket " + bucketName + ": " + e.getMessage());
            return false;
        }
    }


    // OTTIENE LO SPAZIO UTILIZZATO NEL BUCKET
    private long getUsedStorage(String bucketName) {
        S3Client s3Client = backblazeAccounts.get(bucketName);
        if (s3Client == null) {
            LOGGER.warning("⚠ Nessun client S3 trovato per il bucket: " + bucketName);
            return 0L;
        }

        try {
            long totalSize = s3Client.listObjectsV2(builder -> builder.bucket(bucketName))
                    .contents()
                    .stream()
                    .mapToLong(obj -> obj.size())
                    .sum();

            LOGGER.info("📦 Spazio usato nel bucket " + bucketName + ": " + totalSize + " bytes");
            return totalSize;
        } catch (Exception e) {
            LOGGER.warning("⚠ Errore nel recuperare lo spazio usato per il bucket " + bucketName + ": " + e.getMessage());
            return 0L; // In caso di errore, assumiamo 0 per non bloccare il sistema
        }
    }


    // GENERA PRESIGNED URL
    private String generatePresignedUrl(String fileUrl, String bucketName, String keyId, String applicationKey) {
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create("https://s3.us-east-005.backblazeb2.com"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(keyId, applicationKey)))
                .build()) {

            //Estrai solo il nome del file dall'URL
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            LOGGER.info("🔹 Generazione URL firmato per il file: " + fileName);
            LOGGER.info("🔹 Bucket: " + bucketName);
            LOGGER.info("🔹 KeyID usata: " + keyId);
            LOGGER.info("🔹 ApplicationKey usata: " + applicationKey);

            // Crea la richiesta per ottenere il file
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            // Genera il link firmato
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(r ->
                    r.signatureDuration(Duration.ofHours(1))
                            .getObjectRequest(getObjectRequest));

            String signedUrl = presignedRequest.url().toString();
            LOGGER.info("✅ URL firmato generato: " + signedUrl);

            return signedUrl;
        } catch (Exception e) {
            LOGGER.severe("❌ Errore nella generazione del link firmato: " + e.getMessage());
            throw new InternalServerErrorException("Errore nella generazione del link firmato.");
        }
    }


    // CARICA FILE NEL DB(BACKBLAZE B2)
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

}