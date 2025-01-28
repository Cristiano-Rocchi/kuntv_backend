package kun.kuntv_backend.services;

import kun.kuntv_backend.entities.Collection;
import kun.kuntv_backend.entities.Film;
import kun.kuntv_backend.enums.CollectionType;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.repositories.CollectionRepository;
import kun.kuntv_backend.repositories.FilmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;


import java.net.URI;
import java.time.Duration;

import java.io.*;
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
    private S3Client s3Client; // Iniettato dal configuratore

    @Value("${backblaze.b2.keyId}")
    private String keyId;

    @Value("${backblaze.b2.applicationKey}")
    private String applicationKey;


    @Value("${backblaze.b2.bucketName}")
    private String b2BucketName; // Nome del bucket Backblaze B2

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

            LOGGER.info("File originale ricevuto: " + tempFile.getAbsolutePath() + " - Dimensione: " + tempFile.length() / 1024 + " KB");

            // 🔹 Comprimi il video con FFmpeg prima di caricarlo
            compressedFile = compressVideo(tempFile);

            // 🔹 Caricare il video su Backblaze B2
            String uploadedUrl = uploadToBackblazeB2(compressedFile);
            if (uploadedUrl != null) {
                // Genera il link firmato per il file caricato
                String presignedUrl = generatePresignedUrl(uploadedUrl);

                // Imposta l'URL firmato nel film
                film.setVideoUrl(presignedUrl);
                LOGGER.info("Video caricato su Backblaze B2 e link firmato generato: " + presignedUrl);

                // Salva il film con il link firmato
                return filmRepository.save(film);
            }

        } catch (Exception e) {
            LOGGER.severe("Errore nella gestione del file: " + e.getMessage());
            throw new InternalServerErrorException("Errore nella gestione del file: " + e.getMessage());
        } finally {
            // Pulizia dei file temporanei
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            if (compressedFile != null && compressedFile.exists()) {
                compressedFile.delete();
            }
        }

        // Aggiungi un return per garantire che venga restituito un oggetto Film
        return film; // Puoi restituire un film vuoto se necessario
    }

    // Metodo per generare il link firmato
    private String generatePresignedUrl(String fileUrl) {
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.US_EAST_1) // Imposta la regione corretta
                .endpointOverride(URI.create("https://s3.us-east-005.backblazeb2.com")) // Endpoint di Backblaze
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(keyId, applicationKey)))
                .build()) {

            // 🔹 Estrai solo il nome del file dall'URL
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            LOGGER.info("Generazione URL firmato per il file: " + fileName);
            LOGGER.info("Bucket: " + b2BucketName);

            // Crea la richiesta per ottenere il file
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(b2BucketName)
                    .key(fileName) // Passa solo il nome file
                    .build();

            // Genera il link firmato
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(r ->
                    r.signatureDuration(Duration.ofHours(1))
                            .getObjectRequest(getObjectRequest));

            String signedUrl = presignedRequest.url().toString();
            LOGGER.info("URL firmato generato: " + signedUrl);

            return signedUrl;
        } catch (Exception e) {
            LOGGER.severe("Errore nella generazione del link firmato: " + e.getMessage());
            throw new InternalServerErrorException("Errore nella generazione del link firmato.");
        }
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

    /**
     * 🔹 Metodo per caricare un file su Backblaze B2
     */
    private String uploadToBackblazeB2(File file) throws IOException {
        // Usa direttamente il client S3 iniettato
        PutObjectRequest uploadRequest = PutObjectRequest.builder()
                .bucket(b2BucketName) // Usa il nome del bucket
                .key(file.getName()) // Nome del file nel bucket
                .build();

        // Carica il file
        PutObjectResponse response = s3Client.putObject(uploadRequest, RequestBody.fromFile(file));

        // Restituisci l'URL pubblico del file caricato
        return s3Client.utilities().getUrl(builder -> builder.bucket(b2BucketName).key(file.getName())).toString();
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
