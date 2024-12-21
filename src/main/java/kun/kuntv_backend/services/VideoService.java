package kun.kuntv_backend.services;

import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.payloads.VideoRespDTO;
import kun.kuntv_backend.repositories.VideoRepository;
import kun.kuntv_backend.repositories.StagioneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private StagioneRepository stagioneRepository;

    // Ottieni tutti i video
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



    // Ottieni un video per ID
    public Video getVideoById(UUID id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ID del video non valido: " + id));
    }


    // Ottieni video per sezione
    public List<Video> getVideosBySezioneId(UUID sezioneId) {
        return videoRepository.findBySezioneId(sezioneId);
    }

    // Crea un nuovo video (solo admin)
    public Video createVideo(UUID stagioneId, Video video) {
        // Recupera la stagione usando l'ID fornito
        Stagione stagione = stagioneRepository.findById(stagioneId)
                .orElseThrow(() -> new NotFoundException("Stagione non trovata con l'ID: " + stagioneId));

        Sezione sezione = stagione.getSezione(); // Ottieni la sezione associata alla stagione
        video.setStagione(stagione);
        video.setSezione(sezione);

        try {
            return videoRepository.save(video);
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante la creazione del video.");
        }
    }

    // Modifica un video esistente (solo admin)
    public Video updateVideo(UUID id, Video updatedVideo) {
        Video existingVideo = videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Video non trovato con ID: " + id));

        // Aggiorna i dettagli del video
        existingVideo.setTitolo(updatedVideo.getTitolo());
        existingVideo.setDurata(updatedVideo.getDurata());
        existingVideo.setFileLink(updatedVideo.getFileLink());

        // Se è stata fornita una nuova stagione, aggiorna anche la sezione
        if (updatedVideo.getStagione() != null) {
            Stagione stagione = stagioneRepository.findById(updatedVideo.getStagione().getId())
                    .orElseThrow(() -> new NotFoundException("Stagione non trovata con ID: " + updatedVideo.getStagione().getId()));
            existingVideo.setStagione(stagione);
            existingVideo.setSezione(stagione.getSezione()); // Associa automaticamente la sezione della stagione
        }

        try {
            return videoRepository.save(existingVideo);
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante l'aggiornamento del video.");
        }
    }

    // Cancella un video (solo admin)
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
