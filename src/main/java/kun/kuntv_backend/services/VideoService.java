package kun.kuntv_backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private StagioneRepository stagioneRepository;

    @Autowired
    private Cloudinary cloudinary;

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

        try {
            // Carica il file su Cloudinary
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("resource_type", "video"));

            // Ottieni il link del file caricato
            String fileLink = (String) uploadResult.get("secure_url");
            video.setFileLink(fileLink);

            return videoRepository.save(video);
        } catch (IOException e) {
            throw new InternalServerErrorException("Errore durante il caricamento del video su Cloudinary: " + e.getMessage(), e);
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
