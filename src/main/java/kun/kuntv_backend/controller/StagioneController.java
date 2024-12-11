package kun.kuntv_backend.controller;

import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.services.StagioneService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stagioni")
public class StagioneController {

    private final StagioneService stagioneService;

    public StagioneController(StagioneService stagioneService) {
        this.stagioneService = stagioneService;
    }

    // Visualizzazione di tutte le stagioni (user e admin)
    @GetMapping
    public ResponseEntity<List<Stagione>> getAllStagioni() {
        return ResponseEntity.ok(stagioneService.getAllStagioni());
    }

    // Visualizzazione di una stagione per ID (user e admin)
    @GetMapping("/{id}")
    public ResponseEntity<Stagione> getStagioneById(@PathVariable UUID id) {
        return stagioneService.getStagioneById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Visualizzazione di stagioni per una sezione (user e admin)
    @GetMapping("/sezione/{sezioneId}")
    public ResponseEntity<List<Stagione>> getStagioniBySezione(@PathVariable UUID sezioneId) {
        return ResponseEntity.ok(stagioneService.getStagioniBySezioneId(sezioneId));
    }

    // Creazione di una nuova stagione (solo admin)
    @PreAuthorize("hasRole('admin')")
    @PostMapping
    public ResponseEntity<Stagione> createStagione(@RequestBody Stagione stagione) {
        return ResponseEntity.ok(stagioneService.createStagione(stagione));
    }

    // Modifica di una stagione esistente (solo admin)
    @PreAuthorize("hasRole('admin')")
    @PutMapping("/{id}")
    public ResponseEntity<Stagione> updateStagione(@PathVariable UUID id, @RequestBody Stagione stagione) {
        return stagioneService.updateStagione(id, stagione)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Cancellazione di una stagione (solo admin)
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStagione(@PathVariable UUID id) {
        if (stagioneService.deleteStagione(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
