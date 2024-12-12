package kun.kuntv_backend.services;

import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.repositories.SezioneRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SezioneService {

    private final SezioneRepository sezioneRepository;

    public SezioneService(SezioneRepository sezioneRepository) {
        this.sezioneRepository = sezioneRepository;
    }

    // Ottieni tutte le sezioni (accessibile da tutti)
    public List<Sezione> getAllSezioni() {
        return sezioneRepository.findAll();
    }

    // Ottieni una sezione per ID (accessibile da tutti)
    public Optional<Sezione> getSezioneById(UUID id) {
        return sezioneRepository.findById(id);
    }

    // Crea una nuova sezione (solo admin)
    public Sezione createSezione(Sezione sezione) {
        return sezioneRepository.save(sezione);
    }

    // Modifica una sezione esistente (solo admin)
    public Optional<Sezione> updateSezione(UUID id, Sezione updatedSezione) {
        return sezioneRepository.findById(id).map(sezione -> {
            sezione.setTitolo(updatedSezione.getTitolo());
            sezione.setFotoUrl(updatedSezione.getFotoUrl());
            sezione.setAnno(updatedSezione.getAnno());
            sezione.setTag(updatedSezione.getTag());
            return sezioneRepository.save(sezione);
        });
    }

    // Cancella una sezione (solo admin)
    public boolean deleteSezione(UUID id) {
        if (sezioneRepository.existsById(id)) {
            sezioneRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
