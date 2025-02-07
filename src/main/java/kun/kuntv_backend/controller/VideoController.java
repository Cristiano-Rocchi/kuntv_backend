package kun.kuntv_backend.controller;

import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.payloads.NewVideoDTO;
import kun.kuntv_backend.payloads.VideoRespDTO;
import kun.kuntv_backend.repositories.VideoRepository;
import kun.kuntv_backend.services.VideoService;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoRepository videoRepository;

    // Visualizzazione di tutti i video (user e admin)
    @GetMapping
    public ResponseEntity<List<VideoRespDTO>> getAllVideos(
            @RequestParam(required = false) String titolo,
            @RequestParam(required = false) String sezione,
            @RequestParam(required = false) String stagione,
            @RequestParam(required = false) String bucket) {

        List<VideoRespDTO> videos = videoService.getAllVideos(titolo, sezione, stagione, bucket);
        return ResponseEntity.ok(videos);
    }


    // Visualizzazione di un video per ID (user e admin)
    @GetMapping("/{id}")
    public ResponseEntity<VideoRespDTO> getVideoById(@PathVariable UUID id) {
        VideoRespDTO response = videoService.getVideoById(id); // Ora usa direttamente il DTO
        return ResponseEntity.ok(response);
    }


    // Creazione di un nuovo video con caricamento su Cloudinary (solo admin)
    @PostMapping("/upload")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<VideoRespDTO> createVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam String titolo,
            @RequestParam String durata,
            @RequestParam UUID stagioneId) {
        try {
            // Crea un oggetto NewVideoDTO dai parametri
            NewVideoDTO newVideoDTO = new NewVideoDTO();
            newVideoDTO.setTitolo(titolo);
            newVideoDTO.setDurata(durata);
            newVideoDTO.setStagioneId(stagioneId);

            // Chiama il servizio per creare il video
            VideoRespDTO createdVideo = videoService.createVideo(newVideoDTO, file);

            return ResponseEntity.status(201).body(createdVideo);
        } catch (NotFoundException e) {
            return ResponseEntity.status(400).body(null); // Stagione non trovata
        } catch (InternalServerErrorException e) {
            return ResponseEntity.status(500).body(null); // Errore interno
        }
    }




    // Modifica di un video esistente (solo admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Video> updateVideo(@PathVariable UUID id, @RequestBody Video video) {
        try {
            // Recupera il video esistente
            Video existingVideo = videoRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Video non trovato con ID: " + id));


            // Aggiorna solo i campi forniti
            if (video.getTitolo() != null) {
                existingVideo.setTitolo(video.getTitolo());
            }
            if (video.getDurata() != null) {
                existingVideo.setDurata(video.getDurata());
            }

            // Salva e restituisci il video aggiornato
            Video updatedVideo = videoService.updateVideo(id, existingVideo);
            return ResponseEntity.ok(updatedVideo);
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).build(); // Video non trovato
        } catch (Exception e) {
            return ResponseEntity.status(500).build(); // Errore interno
        }
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
