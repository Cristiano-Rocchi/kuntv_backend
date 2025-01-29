package kun.kuntv_backend.services;

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
import software.amazon.awssdk.core.sync.RequestBody;
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
    private Map<String, S3Client> backblazeAccounts; // Multiaccount

    public Film createFilm(Film film, CollectionType tipo, MultipartFile file) {
        // Trova o crea una nuova collezione per il film
        Collection collection = collectionRepository.findByTipo(tipo)
                .orElseGet(() -> {
                    Collection newCollection = new Collection();
                    newCollection.setTipo(tipo);
                    return collectionRepository.save(newCollection);
                });

        film.setCollection(collection);
        String bucketName = collection.getTipo().name().toLowerCase(); // Usa il tipo di collezione come bucketName

        // Ottieni il client S3 associato al bucket
        S3Client s3Client = backblazeAccounts.get(bucketName);
        if (s3Client == null) {
            throw new InternalServerErrorException("❌ Nessun account Backblaze trovato per il bucket: " + bucketName);
        }

        File tempFile = null;
        File compressedFile = null;

        try {
            // Salva il file ricevuto in un file temporaneo
            tempFile = File.createTempFile("upload_", file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }

            LOGGER.info("📁 File originale ricevuto: " + tempFile.getAbsolutePath());

            // 🔹 Comprimi il video con FFmpeg prima di caricarlo
            compressedFile = compressVideo(tempFile);

            // 🔹 Carica il video su Backblaze B2
            String uploadedUrl = uploadToBackblazeB2(s3Client, bucketName, compressedFile);

            // 🔹 Genera il link firmato
            String presignedUrl = generatePresignedUrl(s3Client, bucketName, uploadedUrl);

            // 🔹 Imposta l'URL firmato nel film
            film.setVideoUrl(presignedUrl);
            LOGGER.info("✅ Video caricato su Backblaze B2 e link firmato generato: " + presignedUrl);

            // Salva il film con il link firmato
            return filmRepository.save(film);

        } catch (Exception e) {
            throw new InternalServerErrorException("❌ Errore nella gestione del file: " + e.getMessage());
        } finally {
            if (tempFile != null) tempFile.delete();
            if (compressedFile != null) compressedFile.delete();
        }
    }

    /**
     * 🔹 Metodo per generare il link firmato
     */
    private String generatePresignedUrl(S3Client s3Client, String bucketName, String fileUrl) {
        try (S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create("https://s3.us-east-005.backblazeb2.com"))
                .build()) {

            // 🔹 Estrai solo il nome del file dall'URL
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(r ->
                    r.signatureDuration(Duration.ofHours(1))
                            .getObjectRequest(getObjectRequest));

            String signedUrl = presignedRequest.url().toString();
            LOGGER.info("✅ URL firmato generato: " + signedUrl);
            return signedUrl;
        } catch (Exception e) {
            throw new InternalServerErrorException("❌ Errore nella generazione del link firmato: " + e.getMessage());
        }
    }

    /**
     * 🔹 Metodo per caricare un file su Backblaze B2
     */
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

    /**
     * 🔹 Metodo per comprimere il video con FFmpeg
     */
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

    public Optional<Film> getFilmById(Long id) {
        return filmRepository.findById(id);
    }

    public List<Film> getAllFilms() {
        return filmRepository.findAll();
    }

    public Film updateFilm(Film film) {
        return filmRepository.save(film);
    }

    public void deleteFilm(Long id) {
        if (!filmRepository.existsById(id)) {
            throw new NotFoundException("Film not found with ID: " + id);
        }
        filmRepository.deleteById(id);
    }
}
