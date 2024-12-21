package kun.kuntv_backend.controller;

import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.payloads.VideoRespDTO;
import kun.kuntv_backend.services.VideoService;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    @Autowired
    private VideoService videoService;

    // Visualizzazione di tutti i video (user e admin)
    @GetMapping
    public ResponseEntity<List<VideoRespDTO>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAllVideos());
    }

    // Visualizzazione di un video per ID (user e admin)
    @GetMapping("/{id}")
    public ResponseEntity<VideoRespDTO> getVideoById(@PathVariable UUID id) {
        Video video = videoService.getVideoById(id); // Solleva NotFoundException automaticamente
        VideoRespDTO response = new VideoRespDTO(
                video.getId(),
                video.getTitolo(),
                video.getDurata(),
                video.getFileLink(),
                video.getStagione() != null ? video.getStagione().getTitolo() : null,
                video.getSezione().getTitolo()
        );
        return ResponseEntity.ok(response);
    }


    // Creazione di un nuovo video (solo admin)
    @PreAuthorize("hasRole('admin')")
    @PostMapping
    public ResponseEntity<Video> createVideo(@RequestParam UUID stagioneId, @RequestBody Video video) {
        try {
            Video createdVideo = videoService.createVideo(stagioneId, video);
            return ResponseEntity.status(201).body(createdVideo);
        } catch (NotFoundException e) {
            return ResponseEntity.status(400).body(null); // Bad Request per stagione non trovata
        } catch (InternalServerErrorException e) {
            return ResponseEntity.status(500).body(null); // Errore interno del server
        }
    }

    // Modifica di un video esistente (solo admin)
    @PreAuthorize("hasRole('admin')")
    @PutMapping("/{id}")
    public ResponseEntity<Video> updateVideo(@PathVariable UUID id, @RequestBody Video video) {
        Video updatedVideo = videoService.updateVideo(id, video); // Solleva NotFoundException se l'ID non esiste
        return ResponseEntity.ok(updatedVideo);
    }


    // Cancellazione di un video (solo admin)
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID id) {
        try {
            if (videoService.deleteVideo(id)) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build(); // Video non trovato
            }
        } catch (InternalServerErrorException e) {
            return ResponseEntity.status(500).build(); // Errore interno del server
        }
    }
}
