package kun.kuntv_backend.services;



import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.payloads.StagioneRespDTO;
import kun.kuntv_backend.repositories.StagioneRepository;
import kun.kuntv_backend.repositories.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StagioneService {


    @Autowired
    private StagioneRepository stagioneRepository;
    @Autowired
    private VideoRepository videoRepository;



    // Ottieni tutte le stagioni
    public List<StagioneRespDTO> getAllStagioni() {
        List<Stagione> stagioni = stagioneRepository.findAll();
        return stagioni.stream()
                .map(stagione -> new StagioneRespDTO(
                        stagione.getId(),
                        stagione.getTitolo(),
                        stagione.getAnno(),
                        stagione.getSezione().getTitolo(),
                        stagione.getVideoList().stream().map(Video::getTitolo).collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
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
    public Optional<Stagione> updateStagione(UUID id, Stagione stagione) {
        return stagioneRepository.findById(id).map(existingStagione -> {
            // Aggiorna i campi dell'entità esistente con i valori dal body
            existingStagione.setTitolo(stagione.getTitolo());
            existingStagione.setAnno(stagione.getAnno());
            existingStagione.setSezione(stagione.getSezione());
            return stagioneRepository.save(existingStagione);
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

