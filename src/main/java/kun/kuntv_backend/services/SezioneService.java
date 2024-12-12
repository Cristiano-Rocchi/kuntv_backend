package kun.kuntv_backend.services;

import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.repositories.SezioneRepository;
import kun.kuntv_backend.repositories.StagioneRepository;
import kun.kuntv_backend.repositories.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SezioneService {

    private final SezioneRepository sezioneRepository;
    private final StagioneRepository stagioneRepository;
    private final VideoRepository videoRepository;

    public SezioneService(SezioneRepository sezioneRepository, StagioneRepository stagioneRepository, VideoRepository videoRepository) {
        this.sezioneRepository = sezioneRepository;
        this.stagioneRepository = stagioneRepository;
        this.videoRepository = videoRepository;
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
        // Verifica che la sezione esista
        if (sezioneRepository.existsById(id)) {
            // Prima elimina tutte le stagioni associate alla sezione
            List<Stagione> stagioni = stagioneRepository.findBySezioneId(id);
            for (Stagione stagione : stagioni) {
                // Per ogni stagione, elimina i video associati
                List<Video> videoList = videoRepository.findByStagioneId(stagione.getId());
                for (Video video : videoList) {
                    videoRepository.delete(video);
                }
                // Poi elimina la stagione
                stagioneRepository.delete(stagione);
            }

            // Finalmente, elimina la sezione
            sezioneRepository.deleteById(id);
            return true;
        }
        return false;
    }

}
