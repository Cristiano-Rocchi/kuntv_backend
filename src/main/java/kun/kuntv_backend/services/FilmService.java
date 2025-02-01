package kun.kuntv_backend.services;

import kun.kuntv_backend.config.BackblazeB2Config;
import kun.kuntv_backend.entities.Collection;
import kun.kuntv_backend.entities.Film;
import kun.kuntv_backend.enums.CollectionType;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.repositories.CollectionRepository;
import kun.kuntv_backend.repositories.FilmRepository;
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

@Service
public class FilmService {

    private static final Logger LOGGER = Logger.getLogger(FilmService.class.getName());

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private BackblazeB2Config backblazeB2Config;

    @Autowired
    private Map<String, S3Client> backblazeAccounts; // Multiaccount

    @Autowired
    private Map<String, String> keyIdMapping;

    //TROVA TUTTI I VIDEO
    public List<Film> getAllFilms() {
        return filmRepository.findAll();
    }


    // TROVA SINGOLO VIDEO
    public Optional<Film> getFilmById(Long id) {
        return filmRepository.findById(id);
    }


    // MODIFICA VIDEO
    public Film updateFilm(Film film) {
        return filmRepository.save(film);
    }


    //ELIMINA VIDEO
    public void deleteFilm(Long id) {
        if (!filmRepository.existsById(id)) {
            throw new NotFoundException("Film not found with ID: " + id);
        }
        filmRepository.deleteById(id);
    }


    // ESTRAI NOME BUCKET DALL'URL

    //CREAZIONE VIDEO
    public Film createFilm(Film film, CollectionType tipo, MultipartFile file) {
        // Trova o crea una nuova collezione per il film
        Collection collection = collectionRepository.findByTipo(tipo)
                .orElseGet(() -> {
                    Collection newCollection = new Collection();
                    newCollection.setTipo(tipo);
                    return collectionRepository.save(newCollection);
                });

        film.setCollection(collection);

        File tempFile = null;
        File compressedFile = null;

        try {
            // Salva il file ricevuto in un file temporaneo
            tempFile = File.createTempFile("upload_", file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }

            LOGGER.info("📁 File originale ricevuto: " + tempFile.getAbsolutePath());

            // 🔹 Comprimi il video prima di determinare il bucket
            compressedFile = compressVideo(tempFile);
            long fileSize = compressedFile.length(); // Otteniamo la dimensione del file compresso

            // 📦 🔍 Determina il bucket con spazio disponibile
            String bucketName = determineBucket(collection.getTipo().name().toLowerCase(), fileSize);
            LOGGER.info("📦 Bucket selezionato per il film: " + bucketName);

            // Ottieni il client S3 associato al bucket selezionato
            S3Client s3Client = backblazeAccounts.get(bucketName);
            if (s3Client == null) {
                throw new InternalServerErrorException("❌ Nessun client S3 trovato per il bucket: " + bucketName);
            }

            // 🔹 Carica il file nel bucket selezionato
            String uploadedUrl = uploadToBackblazeB2(s3Client, bucketName, compressedFile);
            LOGGER.info("✅ File caricato su Backblaze B2: " + uploadedUrl);

            // 🔹 Genera il link firmato
            // 🔹 Ottieni le credenziali per il bucket selezionato
            String keyId = backblazeB2Config.keyIdMapping().get(bucketName);
            String applicationKey = backblazeB2Config.applicationKeyMapping().get(bucketName);

            if (keyId == null || applicationKey == null) {
                throw new InternalServerErrorException("❌ Credenziali Backblaze mancanti per il bucket: " + bucketName);
            }

// 🔹 Genera il link firmato
            String presignedUrl = generatePresignedUrl(uploadedUrl, bucketName, keyId, applicationKey);
            LOGGER.info("✅ Link firmato generato: " + presignedUrl);

            // 🔹 Imposta l'URL firmato nel film
            film.setVideoUrl(presignedUrl);

            // Salva il film con il link firmato
            return filmRepository.save(film);

        } catch (Exception e) {
            throw new InternalServerErrorException("❌ Errore nella gestione del file: " + e.getMessage());
        } finally {
            if (tempFile != null) tempFile.delete();
            if (compressedFile != null) compressedFile.delete();
        }
    }

    // COMPRIME VIDEO CON FFMPEG
    private File compressVideo(File inputFile) throws IOException, InterruptedException {
        File compressedFile = new File(inputFile.getParent(), "compressed_" + inputFile.getName());

        String command = String.format("C:\\ffmpeg-7.1-full_build\\bin\\ffmpeg.exe -i \"%s\" -vcodec libx265 -crf 24 \"%s\"",
                inputFile.getAbsolutePath(),
                compressedFile.getAbsolutePath());

        long startTime = System.currentTimeMillis();
        Process process = Runtime.getRuntime().exec(command);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info("FFmpeg: " + line);
            }
        }

        int exitCode = process.waitFor();
        long endTime = System.currentTimeMillis();

        if (exitCode != 0 || !compressedFile.exists() || compressedFile.length() == 0) {
            LOGGER.warning("⚠ Compressione fallita, uso file originale.");
            return inputFile;
        } else {
            LOGGER.info("✅ Compressione completata in " + (endTime - startTime) + " ms");
            return compressedFile;
        }
    }

    // DETERMINA BUCKET CORRETTO DA USARE
    private String determineBucket(String collectionTipo, long fileSize) {
        List<String> buckets = new ArrayList<>(keyIdMapping.keySet());

        if (buckets.isEmpty()) {
            throw new InternalServerErrorException("❌ Nessun bucket disponibile per l'upload!");
        }

        for (String bucket : buckets) {
            if (hasAvailableSpace(bucket, fileSize)) {
                LOGGER.info("✅ Bucket selezionato per il tipo di collezione " + collectionTipo + ": " + bucket);
                return bucket;
            }
        }

        throw new InternalServerErrorException("❌ Nessun bucket ha spazio sufficiente per il file di " + fileSize + " byte.");
    }

    // VERIFICA SPAZIO DISPONIBILE NEL BUCK
    private boolean hasAvailableSpace(String bucketName, long fileSize) {
        S3Client s3Client = backblazeAccounts.get(bucketName);
        if (s3Client == null) {
            LOGGER.warning("⚠ Nessun client S3 trovato per il bucket: " + bucketName);
            return false;
        }

        try {
            // Ottiene i metadati del bucket (Backblaze B2 non fornisce direttamente lo spazio disponibile,
            // quindi qui dovresti avere un metodo per monitorare l'uso dello storage)
            long usedStorage = getUsedStorage(bucketName); // Questa funzione dovrà essere implementata

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

    //OTTIENE LO SPAZIO UTILIZZATO NEL BUCKET
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

            // 🔹 Estrai solo il nome del file dall'URL
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

        try {
            PutObjectRequest uploadRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(file.getName())
                    .build();

            s3Client.putObject(uploadRequest, RequestBody.fromFile(file));

            String fileUrl = "https://" + bucketName + ".s3.us-east-005.backblazeb2.com/" + file.getName();

            LOGGER.info("✅ Upload completato con successo! URL: " + fileUrl);
            return fileUrl;
        } catch (Exception e) {
            LOGGER.severe("❌ Errore durante l'upload su Backblaze B2: " + e.getMessage());
            throw new InternalServerErrorException("Errore durante l'upload su Backblaze B2.");
        }
    }


























}
