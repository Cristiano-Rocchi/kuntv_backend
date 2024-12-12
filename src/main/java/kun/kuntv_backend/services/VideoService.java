package kun.kuntv_backend.services;

import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.repositories.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoService {

    private final VideoRepository videoRepository;

    public VideoService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    // Ottieni tutti i video
    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

    // Ottieni video per ID
    public Optional<Video> getVideoById(UUID id) {
        return videoRepository.findById(id);
    }

    // Ottieni video per sezione
    public List<Video> getVideosBySezioneId(UUID sezioneId) {
        return videoRepository.findBySezioneId(sezioneId);
    }

    // Ottieni video per stagione (opzionale)
    public List<Video> getVideosByStagioneId(UUID stagioneId) {
        return videoRepository.findByStagioneId(stagioneId);
    }

    // Crea un nuovo video (solo admin)
    public Video createVideo(Video video) {
        return videoRepository.save(video);
    }

    // Modifica un video esistente (solo admin)
    public Optional<Video> updateVideo(UUID id, Video updatedVideo) {
        return videoRepository.findById(id).map(video -> {
            video.setTitolo(updatedVideo.getTitolo());

            video.setDurata(updatedVideo.getDurata());
            video.setFileLink(updatedVideo.getFileLink());
            video.setSezione(updatedVideo.getSezione());
            video.setStagione(updatedVideo.getStagione());
            return videoRepository.save(video);
        });
    }

    // Cancella un video (solo admin)
    public boolean deleteVideo(UUID id) {
        if (videoRepository.existsById(id)) {
            videoRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
