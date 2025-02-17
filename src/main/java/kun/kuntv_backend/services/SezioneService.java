package kun.kuntv_backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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

    @PersistenceContext
    private EntityManager entityManager;

    // Ottieni tutte le sezioni (accessibile da tutti)
    public List<SezioneRespDTO> getAllSezioni(String titolo, List<String> tagStrings, String anno) {
        List<Sezione> sezioni;

        // Se sono stati selezionati più tag
        if (tagStrings != null && !tagStrings.isEmpty()) {
            List<TagSezione> tagEnumList = tagStrings.stream()
                    .map(TagSezione::valueOf) // Convertiamo le stringhe nei rispettivi ENUM
                    .collect(Collectors.toList());

            // Usa il metodo che cerca le sezioni con tutti i tag
            sezioni = sezioneRepository.findByMultipleTags(tagEnumList, tagEnumList.size());
        } else {
            // Se non ci sono tag filtriamo tutto
            sezioni = sezioneRepository.findAll();
        }

        // Filtra per titolo se specificato
        if (titolo != null && !titolo.isEmpty()) {
            sezioni.retainAll(sezioneRepository.findByTitoloContainingIgnoreCase(titolo));
        }

        // Filtra per anno se specificato
        if (anno != null && !anno.isEmpty()) {
            sezioni.retainAll(sezioneRepository.findByAnno(anno));
        }

        // Convertiamo le entità in DTO
        return sezioni.stream()
                .map(sezione -> new SezioneRespDTO(
                        sezione.getId(),
                        sezione.getTitolo(),
                        sezione.getFotoUrl(),
                        sezione.getTag(),
                        sezione.getAnno(),
                        sezione.getStagioni().stream().map(Stagione::getTitolo).collect(Collectors.toList()),
                        sezione.getVideoList().stream().map(Video::getTitolo).collect(Collectors.toList()),
                        sezione.getCollection().getId(),
                        sezione.getCollection().getTipo().name()
                ))
                .collect(Collectors.toList());
    }

    // Ottieni una sezione per ID (accessibile da tutti)
    public Sezione getSezioneById(UUID id) {
        return sezioneRepository.findById(id).orElseThrow(() -> new NotFoundException("Sezione non trovata con ID: " + id));
    }

    //Ottieni una sezione per titolo (accessibile da tutti)
    public Sezione getSezioneByTitolo(String titolo) {
        return sezioneRepository.findByTitoloIgnoreCase(titolo)
                .orElseThrow(() -> new NotFoundException("Sezione non trovata"));
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
    public Sezione updateSezione(UUID id, String titolo, String anno, List<TagSezione> tag, MultipartFile file) {
        Sezione sezione = sezioneRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sezione non trovata con ID: " + id));

        try {
            // ✅ Aggiorna solo i campi inviati (gli altri restano invariati)
            if (titolo != null) sezione.setTitolo(titolo);
            if (anno != null) sezione.setAnno(anno);
            if (tag != null) sezione.setTag(tag);

            if (file != null && !file.isEmpty()) {
                Exception lastException = null;
                for (Cloudinary cloudinary : cloudinaryAccounts) {
                    try {
                        // Carica solo se una nuova immagine è stata fornita
                        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                                ObjectUtils.asMap("resource_type", "image"));

                        // ✅ Aggiorna solo se il caricamento ha successo
                        String newFotoUrl = (String) uploadResult.get("secure_url");
                        sezione.setFotoUrl(newFotoUrl);
                        break; // Se il caricamento è ok, interrompi il loop
                    } catch (IOException e) {
                        lastException = e;
                    }
                }
                if (sezione.getFotoUrl() == null) {
                    throw new InternalServerErrorException("Errore nel caricamento dell'immagine su Cloudinary.");
                }
            }

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



    // aggiorna gli enum tag
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void updateTagConstraint() {
        // Ottieni tutti i valori dell'enum TagSezione
        String enumValues = String.join("', '",
                java.util.Arrays.stream(TagSezione.values())
                        .map(Enum::name)
                        .toArray(String[]::new));

        // Query per eliminare il vecchio vincolo
        String dropConstraintQuery = "ALTER TABLE sezione_tag DROP CONSTRAINT IF EXISTS sezione_tag_tag_check";

        // Query per creare il nuovo vincolo con gli ENUM aggiornati
        String addConstraintQuery = "ALTER TABLE sezione_tag ADD CONSTRAINT sezione_tag_tag_check " +
                "CHECK (tag IN ('" + enumValues + "'))";

        // Esegui le query dentro una transazione attiva
        entityManager.createNativeQuery(dropConstraintQuery).executeUpdate();
        entityManager.createNativeQuery(addConstraintQuery).executeUpdate();


    }


}
