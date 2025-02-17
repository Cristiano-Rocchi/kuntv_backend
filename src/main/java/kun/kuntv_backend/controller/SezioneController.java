package kun.kuntv_backend.controller;

import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.enums.CollectionType;

import kun.kuntv_backend.enums.TagSezione;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import kun.kuntv_backend.payloads.NewSezioneDTO;
import kun.kuntv_backend.payloads.SezioneRespDTO;
import kun.kuntv_backend.services.SezioneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sezioni")
public class SezioneController {

    @Autowired
    private SezioneService sezioneService;

    // Visualizzazione di tutte le sezioni (user e admin)
    @GetMapping
    public ResponseEntity<List<SezioneRespDTO>> getAllSezioni(
            @RequestParam(required = false) String titolo,
            @RequestParam(required = false) List<String> tag,
            @RequestParam(required = false) String anno) {

        List<SezioneRespDTO> sezioni = sezioneService.getAllSezioni(titolo, tag, anno);
        return ResponseEntity.ok(sezioni);
    }


    // Visualizzazione di una sezione per ID (user e admin)
    @GetMapping("/{id}")
    public ResponseEntity<Sezione> getSezioneById(@PathVariable UUID id) {
        Sezione sezione = sezioneService.getSezioneById(id);
        return ResponseEntity.ok(sezione);
    }
    // Visualizza sezione per TITOLO
    @GetMapping("/titolo/{titolo}")
    public ResponseEntity<Sezione> getSezioneByTitolo(@PathVariable String titolo) {
        Sezione sezione = sezioneService.getSezioneByTitolo(titolo);
        return ResponseEntity.ok(sezione);
    }


    // Creazione di una sezione associata a una Collection (solo admin)
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Sezione> createSezione(@ModelAttribute NewSezioneDTO newSezioneDTO) {
        try {
            // Creiamo un oggetto Sezione
            Sezione sezione = new Sezione();
            sezione.setTitolo(newSezioneDTO.getTitolo());

            // Convertiamo la lista di stringhe in una lista di enum
            sezione.setTag(newSezioneDTO.getTagAsEnumList());

            sezione.setAnno(newSezioneDTO.getAnno());

            // Passiamo i dati e il file al servizio
            Sezione createdSezione = sezioneService.createSezione(
                    sezione,
                    CollectionType.SERIE_TV,
                    newSezioneDTO.getFile()
            );
            return ResponseEntity.status(201).body(createdSezione);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(null); // Gestione errore se il tag non è valido
        } catch (InternalServerErrorException e) {
            return ResponseEntity.status(500).body(null);
        }
    }


    // Modifica di una sezione esistente (solo admin)
    @PreAuthorize("hasRole('admin')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Sezione> updateSezione(
            @PathVariable UUID id,
            @RequestParam(value = "titolo", required = false) String titolo,
            @RequestParam(value = "anno", required = false) String anno,
            @RequestParam(value = "tag", required = false) List<TagSezione> tag,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        Sezione updatedSezione = sezioneService.updateSezione(id, titolo, anno, tag, file);
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

    @GetMapping("/tags")
    public ResponseEntity<TagSezione[]> getTagSezione() {
        return ResponseEntity.ok(TagSezione.values());
    }

}
