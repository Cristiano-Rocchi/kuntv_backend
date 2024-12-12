package kun.kuntv_backend.controller;

import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.services.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    // Visualizzazione di tutti i video (user e admin)
    @GetMapping
    public ResponseEntity<List<Video>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAllVideos());
    }

    // Visualizzazione di un video per ID (user e admin)
    @GetMapping("/{id}")
    public ResponseEntity<Video> getVideoById(@PathVariable UUID id) {
        return videoService.getVideoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Visualizzazione di video per una sezione (user e admin)
    @GetMapping("/sezione/{sezioneId}")
    public ResponseEntity<List<Video>> getVideosBySezione(@PathVariable UUID sezioneId) {
        return ResponseEntity.ok(videoService.getVideosBySezioneId(sezioneId));
    }

    // Creazione di un nuovo video (solo admin)
    @PreAuthorize("hasRole('admin')")
    @PostMapping
    public ResponseEntity<Video> createVideo(@RequestBody Video video) {
        return ResponseEntity.ok(videoService.createVideo(video));
    }

    // Modifica di un video esistente (solo admin)
    @PreAuthorize("hasRole('admin')")
    @PutMapping("/{id}")
    public ResponseEntity<Video> updateVideo(@PathVariable UUID id, @RequestBody Video video) {
        return videoService.updateVideo(id, video)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Cancellazione di un video (solo admin)
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID id) {
        if (videoService.deleteVideo(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
