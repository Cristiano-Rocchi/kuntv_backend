package kun.kuntv_backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import kun.kuntv_backend.entities.Collection;
import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.enums.CollectionType;
import kun.kuntv_backend.enums.TagSezione;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.payloads.SezioneRespDTO;
import kun.kuntv_backend.repositories.CollectionRepository;
import kun.kuntv_backend.repositories.SezioneRepository;
import kun.kuntv_backend.repositories.StagioneRepository;
import kun.kuntv_backend.repositories.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
    @Autowired
    private StagioneService stagioneService;

    @Autowired
    private List<Cloudinary> cloudinaryAccounts;

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

    // Creazione di una nuova sezione con caricamento immagine su Cloudinary(solo admin)
    public Sezione createSezione(Sezione sezione, CollectionType tipo, MultipartFile file) {
        // Ottieni o crea la Collection associata
        Collection collection = collectionRepository.findByTipo(tipo)
                .orElseGet(() -> {
                    Collection newCollection = new Collection();
                    newCollection.setTipo(tipo);
                    return collectionRepository.save(newCollection);
                });

        sezione.setCollection(collection);

        Exception lastException = null;
        for (Cloudinary cloudinary : cloudinaryAccounts) {
            try {
                // Carica l'immagine sul primo account disponibile
                Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                        ObjectUtils.asMap("resource_type", "image"));

                // Ottieni il link dell'immagine caricata
                String fileLink = (String) uploadResult.get("secure_url");
                sezione.setFotoUrl(fileLink);

                return sezioneRepository.save(sezione);
            } catch (IOException e) {
                lastException = e; // Prova con il prossimo account
            }
        }

        throw new InternalServerErrorException("Errore durante il caricamento dell'immagine su Cloudinary: " +
                (lastException != null ? lastException.getMessage() : "Nessun account disponibile"));
    }

    // Modifica una sezione esistente (solo admin)
    public Sezione updateSezione(UUID id, Sezione updatedSezione) {
        Sezione sezione = sezioneRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sezione non trovata con ID: " + id));
        try {
            sezione.setTitolo(updatedSezione.getTitolo());
            sezione.setFotoUrl(updatedSezione.getFotoUrl());
            sezione.setAnno(updatedSezione.getAnno());
            sezione.setTag(updatedSezione.getTag()); // Ora è una lista di TagSezione
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
            // Recupera tutte le stagioni di questa sezione
            List<Stagione> stagioni = stagioneRepository.findBySezioneId(id);

            // Per ogni stagione, chiama deleteStagione(...) invece di fare tutto a mano
            for (Stagione stagione : stagioni) {
                // Questo metodo, se l'hai modificato come da consiglio precedente,
                // si occuperà anche di eliminare i file su Backblaze B2
                stagioneService.deleteStagione(stagione.getId());
            }

            // Infine, elimina la sezione
            sezioneRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante la cancellazione della sezione.");
        }
    }
}
