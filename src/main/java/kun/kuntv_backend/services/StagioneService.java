package kun.kuntv_backend.services;

import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.payloads.StagioneRespDTO;
import kun.kuntv_backend.payloads.VideoRespDTO;
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
    // Ottieni i video per stagione
    public List<VideoRespDTO> getVideosByStagioneId(UUID stagioneId) {
        List<Video> videos = videoRepository.findByStagioneId(stagioneId);

        // Converte l'elenco di Video in VideoRespDTO
        return videos.stream()
                .map(video -> new VideoRespDTO(
                        video.getId(),
                        video.getTitolo(),
                        video.getDurata(),
                        video.getFileLink(),
                        video.getStagione() != null ? video.getStagione().getTitolo() : null,
                        video.getSezione().getTitolo()
                ))
                .collect(Collectors.toList());
    }


    // Ottieni una stagione per ID
    public Stagione getStagioneById(UUID id) {
        return stagioneRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stagione non trovata con ID: " + id));
    }

    // Ottieni stagioni per una sezione
    public List<Stagione> getStagioniBySezioneId(UUID sezioneId) {
        return stagioneRepository.findBySezioneId(sezioneId);
    }

    // Crea una nuova stagione (solo admin)
    public Stagione createStagione(Stagione stagione) {
        try {
            return stagioneRepository.save(stagione);
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante la creazione della stagione.");
        }
    }

    // Modifica una stagione esistente (solo admin)
    public Stagione updateStagione(UUID id, Stagione stagione) {
        Stagione existingStagione = stagioneRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stagione non trovata con ID: " + id));
        try {
            existingStagione.setTitolo(stagione.getTitolo());
            existingStagione.setAnno(stagione.getAnno());
            existingStagione.setSezione(stagione.getSezione());
            return stagioneRepository.save(existingStagione);
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante l'aggiornamento della stagione.");
        }
    }

    // Cancella una stagione (solo admin)
    public boolean deleteStagione(UUID id) {
        if (!stagioneRepository.existsById(id)) {
            throw new NotFoundException("Stagione non trovata con ID: " + id);
        }
        try {
            Stagione stagione = stagioneRepository.findById(id).orElse(null);

            if (stagione != null) {
                List<Video> videoList = videoRepository.findByStagioneId(id);
                for (Video video : videoList) {
                    videoRepository.delete(video);
                }
            }

            stagioneRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante la cancellazione della stagione.");
        }
    }
}
