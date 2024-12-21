package kun.kuntv_backend.controller;

import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.payloads.SezioneRespDTO;
import kun.kuntv_backend.services.SezioneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sezioni")
public class SezioneController {

    @Autowired
    private SezioneService sezioneService;

    // Visualizzazione di tutte le sezioni (user e admin)
    @GetMapping
    public ResponseEntity<List<SezioneRespDTO>> getAllSezioni() {
        return ResponseEntity.ok(sezioneService.getAllSezioni());
    }

    // Visualizzazione di una sezione per ID (user e admin)
    @GetMapping("/{id}")
    public ResponseEntity<Sezione> getSezioneById(@PathVariable UUID id) {
        Sezione sezione = sezioneService.getSezioneById(id); // Solleva NotFoundException se non trova la sezione
        return ResponseEntity.ok(sezione);
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
        Sezione updatedSezione = sezioneService.updateSezione(id, sezione); // Solleva NotFoundException se l'ID non esiste
        return ResponseEntity.ok(updatedSezione);
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
