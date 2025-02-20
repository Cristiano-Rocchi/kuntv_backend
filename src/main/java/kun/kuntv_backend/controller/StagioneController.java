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

    // 📌 Ottieni tutte le stagioni
    @GetMapping
    public ResponseEntity<List<StagioneRespDTO>> getAllStagioni(
            @RequestParam(required = false) String titolo,
            @RequestParam(required = false) String sezione,
            @RequestParam(required = false) String anno) {
        return ResponseEntity.ok(stagioneService.getAllStagioni(titolo, sezione, anno));
    }


    // 📌 Ottieni una stagione per ID
    @GetMapping("/{id}")
    public ResponseEntity<StagioneRespDTO> getStagioneById(@PathVariable UUID id) {
        return ResponseEntity.ok(stagioneService.getStagioneById(id));
    }

    // 📌 Ottieni tutte le stagioni di una sezione
    @GetMapping("/sezione/{sezioneId}")
    public ResponseEntity<List<StagioneRespDTO>> getStagioniBySezione(@PathVariable UUID sezioneId) {
        return ResponseEntity.ok(stagioneService.getStagioniBySezioneId(sezioneId));
    }

    // 📌 Crea una nuova stagione (simile a createSezione)
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<StagioneRespDTO> createStagione(@ModelAttribute NewStagioneDTO newStagioneDTO) {
        return ResponseEntity.ok(stagioneService.createStagione(newStagioneDTO, newStagioneDTO.getImmagine()));
    }

    // 📌 Modifica di una stagione
    @PreAuthorize("hasRole('admin')")
    @PutMapping("/{id}")
    public ResponseEntity<StagioneRespDTO> updateStagione(@PathVariable UUID id, @ModelAttribute NewStagioneDTO newStagioneDTO) {
        return ResponseEntity.ok(
                stagioneService.updateStagione(
                        id,
                        newStagioneDTO.getTitolo(),  // Estrai il titolo come stringa
                        newStagioneDTO.getAnno(),    // Estrai l'anno come stringa
                        newStagioneDTO.getImmagine() // Passa il file immagine
                )
        );
    }


    // 📌 Cancella una stagione
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStagione(@PathVariable UUID id) {
        if (stagioneService.deleteStagione(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
