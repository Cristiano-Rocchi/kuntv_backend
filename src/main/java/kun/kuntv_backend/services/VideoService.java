package kun.kuntv_backend.services;

import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.payloads.VideoRespDTO;
import kun.kuntv_backend.repositories.VideoRepository;
import kun.kuntv_backend.repositories.StagioneRepository;
import kun.kuntv_backend.controller.GoogleAuthController;
import com.google.api.client.auth.oauth2.Credential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private StagioneRepository stagioneRepository;

    @Autowired
    private GoogleDriveService googleDriveService;

    public List<VideoRespDTO> getAllVideos() {
        List<Video> videos = videoRepository.findAll();
        return videos.stream()
                .map(video -> new VideoRespDTO(
                        video.getId(),
                        video.getTitolo(),
                        video.getDurata(),
                        video.getFileLink(),
                        video.getStagione() != null ? video.getStagione().getTitolo() : null,
                        video.getSezione().getTitolo()
                ))
                .collect(Collectors.toList());
    }

    public Video getVideoById(UUID id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ID del video non valido: " + id));
    }

    public List<Video> getVideosBySezioneId(UUID sezioneId) {
        return videoRepository.findBySezioneId(sezioneId);
    }

    public Video createVideo(UUID stagioneId, Video video, MultipartFile file) {
        Stagione stagione = stagioneRepository.findById(stagioneId)
                .orElseThrow(() -> new NotFoundException("Stagione non trovata con l'ID: " + stagioneId));

        Sezione sezione = stagione.getSezione();
        video.setStagione(stagione);
        video.setSezione(sezione);

        Path tempFile = null;

        try {
            tempFile = Files.createTempFile(file.getOriginalFilename(), null);
            file.transferTo(tempFile.toFile());

            // Ottieni le credenziali
            Credential credential = GoogleAuthController.getCredential();
            if (credential == null) {
                throw new IllegalStateException("Autenticazione non trovata. Devi autorizzarti prima.");
            }

            // Carica su Google Drive
            String driveFileId = googleDriveService.uploadFile(tempFile, file.getOriginalFilename(), file.getContentType(), credential);

            video.setFileLink("https://drive.google.com/file/d/" + driveFileId + "/view");
            return videoRepository.save(video);
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante la creazione del video su Google Drive: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ex) {
                    System.err.println("Impossibile eliminare il file temporaneo: " + ex.getMessage());
                }
            }
        }
    }

    public Video updateVideo(UUID id, Video updatedVideo) {
        Video existingVideo = videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Video non trovato con ID: " + id));

        existingVideo.setTitolo(updatedVideo.getTitolo());
        existingVideo.setDurata(updatedVideo.getDurata());
        existingVideo.setFileLink(updatedVideo.getFileLink());

        if (updatedVideo.getStagione() != null) {
            Stagione stagione = stagioneRepository.findById(updatedVideo.getStagione().getId())
                    .orElseThrow(() -> new NotFoundException("Stagione non trovata con ID: " + updatedVideo.getStagione().getId()));
            existingVideo.setStagione(stagione);
            existingVideo.setSezione(stagione.getSezione());
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
            videoRepository.delete(video);
            return true;
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante la cancellazione del video.");
        }
    }
}
