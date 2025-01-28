package kun.kuntv_backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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

import java.io.*;
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
    private List<Cloudinary> cloudinaryAccounts;

    public Film createFilm(Film film, CollectionType tipo, MultipartFile file) {
        Collection collection = collectionRepository.findByTipo(tipo)
                .orElseGet(() -> {
                    Collection newCollection = new Collection();
                    newCollection.setTipo(tipo);
                    return collectionRepository.save(newCollection);
                });

        film.setCollection(collection);

        File tempFile = null;
        File compressedFile = null;
        List<File> segments = new ArrayList<>();

        try {
            // Salva il file ricevuto in un file temporaneo
            tempFile = File.createTempFile("upload_", file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }

            LOGGER.info("File originale ricevuto: " + tempFile.getAbsolutePath() + " - Dimensione: " + tempFile.length() / 1024 + " KB");

            // 🔹 Comprimi il video con FFmpeg prima di spezzarlo
            compressedFile = compressVideo(tempFile);

            // 🔹 Spezzare il video in segmenti di max 100MB
            segments = splitVideo(compressedFile);

            // 🔹 Caricare i segmenti su Cloudinary
            List<String> uploadedUrls = new ArrayList<>();
            Exception lastException = null;

            for (Cloudinary cloudinary : cloudinaryAccounts) {
                try {
                    for (File segment : segments) {
                        Map<String, Object> uploadOptions = ObjectUtils.asMap(
                                "resource_type", "video",
                                "chunk_size", 6000000, // 6MB chunk size
                                "eager_async", true
                        );
                        Map<String, Object> uploadResult = cloudinary.uploader().upload(segment, uploadOptions);
                        String fileLink = (String) uploadResult.get("public_id");
                        uploadedUrls.add(fileLink);
                        LOGGER.info("Segmento caricato: " + fileLink);
                    }

                    // 🔹 Unire i segmenti su Cloudinary
                    String mergedVideoUrl = concatenateVideos(uploadedUrls, cloudinary);
                    if (mergedVideoUrl != null) {
                        film.setVideoUrl(mergedVideoUrl);
                        LOGGER.info("Video finale unito: " + mergedVideoUrl);
                        return filmRepository.save(film);
                    }
                } catch (IOException e) {
                    lastException = e;
                    LOGGER.warning("Errore durante l'upload su Cloudinary: " + e.getMessage());
                }
            }

            throw new InternalServerErrorException("Errore durante il caricamento del film su Cloudinary: " +
                    (lastException != null ? lastException.getMessage() : "Nessun account disponibile"));
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
            for (File segment : segments) {
                if (segment.exists()) {
                    segment.delete();
                }
            }
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
            return inputFile;
        } else {
            LOGGER.info("Compressione completata: " + compressedFile.getAbsolutePath() +
                    " - Dimensione: " + compressedFile.length() / 1024 + " KB - Tempo impiegato: " + (endTime - startTime) + " ms");
            return compressedFile;
        }
    }

    /**
     * 🔹 Metodo per dividere un video in segmenti da 100MB
     */
    private List<File> splitVideo(File inputFile) throws IOException, InterruptedException {
        List<File> chunks = new ArrayList<>();
        String outputPattern = inputFile.getParent() + File.separator + "segment_%03d.mp4";

        String command = String.format("C:\\ffmpeg-7.1-full_build\\bin\\ffmpeg.exe -i \"%s\" -c copy -map 0 -segment_time 00:04:30 -f segment -reset_timestamps 1 \"%s\"",
                inputFile.getAbsolutePath(), outputPattern);

        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();

        // Recupera i segmenti generati
        File folder = inputFile.getParentFile();
        for (File file : folder.listFiles()) {
            if (file.getName().startsWith("segment_") && file.getName().endsWith(".mp4") && file.length() < 100 * 1024 * 1024) {
                chunks.add(file);
            }
        }
        return chunks;
    }

    /**
     * 🔹 Metodo per unire i segmenti su Cloudinary
     */
    private String concatenateVideos(List<String> publicIds, Cloudinary cloudinary) {
        try {
            Map<String, Object> concatOptions = ObjectUtils.asMap(
                    "resource_type", "video",
                    "public_id", "merged_video_" + UUID.randomUUID(),
                    "eager_async", true,
                    "transformation", ObjectUtils.asMap(
                            "fetch_format", "mp4",
                            "quality", "auto"
                    ),
                    "manifest_transformation", ObjectUtils.asMap(
                            "format", "mp4"
                    )
            );

            StringBuilder transformationString = new StringBuilder();
            for (String id : publicIds) {
                transformationString.append(id).append("/");
            }

            concatOptions.put("manifest", transformationString.toString());

            Map<String, Object> result = cloudinary.uploader().upload(null, concatOptions);
            return (String) result.get("secure_url");

        } catch (Exception e) {
            LOGGER.warning("Errore durante la concatenazione su Cloudinary: " + e.getMessage());
            return null;
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
