package kun.kuntv_backend.controller;

import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.services.SezioneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sezioni")
public class SezioneController {

    private final SezioneService sezioneService;

    public SezioneController(SezioneService sezioneService) {
        this.sezioneService = sezioneService;
    }

    // Visualizzazione di tutte le sezioni (user e admin)
    @GetMapping
    public ResponseEntity<List<Sezione>> getAllSezioni() {
        return ResponseEntity.ok(sezioneService.getAllSezioni());
    }

    // Visualizzazione di una sezione per ID (user e admin)
    @GetMapping("/{id}")
    public ResponseEntity<Sezione> getSezioneById(@PathVariable UUID id) {
        return sezioneService.getSezioneById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Creazione di una sezione (solo admin)
    @PreAuthorize("hasRole('admin')")
    @PostMapping
    public ResponseEntity<Sezione> createSezione(@RequestBody Sezione sezione) {
        return ResponseEntity.ok(sezioneService.createSezione(sezione));
    }

    // Modifica di una sezione (solo admin)
    @PreAuthorize("hasRole('admin')")
    @PutMapping("/{id}")
    public ResponseEntity<Sezione> updateSezione(@PathVariable UUID id, @RequestBody Sezione sezione) {
        return sezioneService.updateSezione(id, sezione)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Cancellazione di una sezione (solo admin)
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSezione(@PathVariable UUID id) {
        if (sezioneService.deleteSezione(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
