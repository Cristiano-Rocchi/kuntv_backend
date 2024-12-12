package kun.kuntv_backend.services;



import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.repositories.StagioneRepository;
import kun.kuntv_backend.repositories.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StagioneService {

    private final StagioneRepository stagioneRepository;
    private final VideoRepository videoRepository;

    public StagioneService(StagioneRepository stagioneRepository, VideoRepository videoRepository) {
        this.stagioneRepository = stagioneRepository;
        this.videoRepository = videoRepository;
    }

    // Ottieni tutte le stagioni
    public List<Stagione> getAllStagioni() {
        return stagioneRepository.findAll();
    }

    // Ottieni una stagione per ID
    public Optional<Stagione> getStagioneById(UUID id) {
        return stagioneRepository.findById(id);
    }

    // Ottieni stagioni per una sezione
    public List<Stagione> getStagioniBySezioneId(UUID sezioneId) {
        return stagioneRepository.findBySezioneId(sezioneId);
    }

    // Crea una nuova stagione (solo admin)
    public Stagione createStagione(Stagione stagione) {
        return stagioneRepository.save(stagione);
    }

    // Modifica una stagione esistente (solo admin)
    public Optional<Stagione> updateStagione(UUID id, Stagione updatedStagione) {
        return stagioneRepository.findById(id).map(stagione -> {
            stagione.setTitolo(updatedStagione.getTitolo());
            stagione.setSezione(updatedStagione.getSezione());
            stagione.setAnno(updatedStagione.getAnno());
            return stagioneRepository.save(stagione);
        });
    }

    // Cancella una stagione (solo admin)
    public boolean deleteStagione(UUID id) {
        // Verifica se la stagione esiste
        if (stagioneRepository.existsById(id)) {
            // Trova la stagione
            Stagione stagione = stagioneRepository.findById(id).orElse(null);

            // Se la stagione esiste, elimina i video associati
            if (stagione != null) {
                List<Video> videoList = videoRepository.findByStagioneId(id);
                for (Video video : videoList) {
                    videoRepository.delete(video); // Elimina il video
                }
            }

            // Elimina la stagione
            stagioneRepository.deleteById(id);
            return true;
        }
        return false;
    }

}

