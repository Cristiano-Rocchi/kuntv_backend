package kun.kuntv_backend.controller;

import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.payloads.NewStagioneDTO;
import kun.kuntv_backend.payloads.StagioneRespDTO;
import kun.kuntv_backend.services.StagioneService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stagioni")
public class StagioneController {

    private final StagioneService stagioneService;

    public StagioneController(StagioneService stagioneService) {
        this.stagioneService = stagioneService;
    }

    // 📌 Ottieni tutte le stagioni (user e admin)
    @GetMapping
    public ResponseEntity<List<StagioneRespDTO>> getAllStagioni() {
        return ResponseEntity.ok(stagioneService.getAllStagioni());
    }

    // 📌 Ottieni una stagione per ID (user e admin)
    @GetMapping("/{id}")
    public ResponseEntity<StagioneRespDTO> getStagioneById(@PathVariable UUID id) {
        return ResponseEntity.ok(stagioneService.getStagioneById(id));
    }

    // 📌 Ottieni tutte le stagioni di una sezione (user e admin)
    @GetMapping("/sezione/{sezioneId}")
    public ResponseEntity<List<StagioneRespDTO>> getStagioniBySezione(@PathVariable UUID sezioneId) {
        return ResponseEntity.ok(stagioneService.getStagioniBySezioneId(sezioneId));
    }

    // 📌 Crea una nuova stagione (supporta upload immagine, solo admin)
    @PreAuthorize("hasRole('admin')")
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<StagioneRespDTO> createStagione(
            @RequestPart("titolo") String titolo,
            @RequestPart("anno") String anno,
            @RequestPart("sezioneId") UUID sezioneId,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        NewStagioneDTO newStagioneDTO = new NewStagioneDTO(titolo, anno, sezioneId, null); // Il file sarà gestito nel service
        return ResponseEntity.ok(stagioneService.createStagione(newStagioneDTO, file));
    }

    // 📌 Modifica di una stagione (supporta upload immagine, solo admin)
    @PreAuthorize("hasRole('admin')")
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<StagioneRespDTO> updateStagione(
            @PathVariable UUID id,
            @RequestPart("titolo") String titolo,
            @RequestPart("anno") String anno,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        return ResponseEntity.ok(stagioneService.updateStagione(id, titolo, anno, file));
    }

    // 📌 Cancella una stagione (solo admin)
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStagione(@PathVariable UUID id) {
        if (stagioneService.deleteStagione(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
