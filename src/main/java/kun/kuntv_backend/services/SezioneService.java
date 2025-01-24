package kun.kuntv_backend.services;

import kun.kuntv_backend.entities.Collection;
import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.enums.CollectionType;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.payloads.SezioneRespDTO;
import kun.kuntv_backend.repositories.CollectionRepository;
import kun.kuntv_backend.repositories.SezioneRepository;
import kun.kuntv_backend.repositories.StagioneRepository;
import kun.kuntv_backend.repositories.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SezioneService {

    @Autowired
    private SezioneRepository sezioneRepository;

    @Autowired
    private StagioneRepository stagioneRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    // Ottieni tutte le sezioni (accessibile da tutti)
    public List<SezioneRespDTO> getAllSezioni() {
        List<Sezione> sezioni = sezioneRepository.findAll();
        return sezioni.stream()
                .map(sezione -> new SezioneRespDTO(
                        sezione.getId(),
                        sezione.getTitolo(),
                        sezione.getFotoUrl(),
                        sezione.getTag(),
                        sezione.getAnno(),
                        sezione.getStagioni().stream().map(Stagione::getTitolo).collect(Collectors.toList()),
                        sezione.getVideoList().stream().map(Video::getTitolo).collect(Collectors.toList()),
                        sezione.getCollection().getId(), // Aggiungiamo l'ID della Collection
                        sezione.getCollection().getTipo().name() // Aggiungiamo il tipo della Collection
                ))
                .collect(Collectors.toList());
    }

    // Ottieni una sezione per ID (accessibile da tutti)
    public Sezione getSezioneById(UUID id) {
        return sezioneRepository.findById(id).orElseThrow(() -> new NotFoundException("Sezione non trovata con ID: " + id));
    }

    // Crea una nuova sezione associata a una Collection o crea dinamicamente la Collection (solo admin)
    public Sezione createSezione(Sezione sezione, CollectionType tipo) {
        Collection collection = collectionRepository.findByTipo(tipo)
                .orElseGet(() -> {
                    Collection newCollection = new Collection();
                    newCollection.setTipo(tipo);
                    return collectionRepository.save(newCollection);
                });

        try {
            sezione.setCollection(collection);
            return sezioneRepository.save(sezione);
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante la creazione della sezione.");
        }
    }

    // Modifica una sezione esistente (solo admin)
    public Sezione updateSezione(UUID id, Sezione updatedSezione) {
        Sezione sezione = sezioneRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sezione non trovata con ID: " + id));
        try {
            sezione.setTitolo(updatedSezione.getTitolo());
            sezione.setFotoUrl(updatedSezione.getFotoUrl());
            sezione.setAnno(updatedSezione.getAnno());
            sezione.setTag(updatedSezione.getTag());
            return sezioneRepository.save(sezione);
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante l'aggiornamento della sezione.");
        }
    }

    // Cancella una sezione (solo admin)
    public boolean deleteSezione(UUID id) {
        if (!sezioneRepository.existsById(id)) {
            throw new NotFoundException("Sezione non trovata con ID: " + id);
        }
        try {
            List<Stagione> stagioni = stagioneRepository.findBySezioneId(id);
            for (Stagione stagione : stagioni) {
                List<Video> videoList = videoRepository.findByStagioneId(stagione.getId());
                videoRepository.deleteAll(videoList);
                stagioneRepository.delete(stagione);
            }
            sezioneRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante la cancellazione della sezione.");
        }
    }
}
