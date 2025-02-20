package kun.kuntv_backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.payloads.NewStagioneDTO;
import kun.kuntv_backend.payloads.StagioneRespDTO;
import kun.kuntv_backend.payloads.VideoRespDTO;
import kun.kuntv_backend.repositories.SezioneRepository;
import kun.kuntv_backend.repositories.StagioneRepository;
import kun.kuntv_backend.repositories.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StagioneService {

    @Autowired
    private StagioneRepository stagioneRepository;
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private SezioneRepository sezioneRepository;
    @Autowired
    private VideoService videoService;
    @Autowired
    private List<Cloudinary> cloudinaryAccounts;

    // 📌 Metodo per scegliere un account Cloudinary in modo bilanciato
    private Cloudinary getCloudinaryInstance() {
        return cloudinaryAccounts.get((int) (Math.random() * cloudinaryAccounts.size()));
    }

    // 📌 Ottieni tutte le stagioni
    public List<StagioneRespDTO> getAllStagioni(String titolo, String sezione, String anno) {
        List<Stagione> filteredStagioni = stagioneRepository.findAll();

        if (titolo != null && !titolo.isEmpty()) {
            filteredStagioni.retainAll(stagioneRepository.findByTitoloContainingIgnoreCase(titolo));
        }
        if (sezione != null && !sezione.isEmpty()) {
            filteredStagioni.retainAll(stagioneRepository.findBySezione_TitoloContainingIgnoreCase(sezione));
        }
        if (anno != null && !anno.isEmpty()) {
            filteredStagioni.retainAll(stagioneRepository.findByAnnoContainingIgnoreCase(anno));
        }

        return filteredStagioni.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    // 📌 Ottieni una stagione per ID
    public StagioneRespDTO getStagioneById(UUID id) {
        Stagione stagione = stagioneRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stagione non trovata con ID: " + id));
        return convertToDTO(stagione);
    }

    // 📌 Ottieni stagioni per una sezione
    public List<StagioneRespDTO> getStagioniBySezioneId(UUID sezioneId) {
        return stagioneRepository.findBySezioneId(sezioneId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 📌 Crea una nuova stagione con supporto per immagine
    public StagioneRespDTO createStagione(NewStagioneDTO dto, MultipartFile immagine) {
        Sezione sezione = sezioneRepository.findById(dto.getSezioneId())
                .orElseThrow(() -> new NotFoundException("Sezione non trovata con ID: " + dto.getSezioneId()));

        Stagione stagione = new Stagione();
        stagione.setTitolo(dto.getTitolo());
        stagione.setAnno(dto.getAnno());
        stagione.setSezione(sezione);

        if (immagine != null && !immagine.isEmpty()) {
            String imageUrl = uploadImageToCloudinary(immagine);
            stagione.setImmagineUrl(imageUrl);
        }

        return convertToDTO(stagioneRepository.save(stagione));
    }

    // 📌 Modifica una stagione esistente (supporta cambio immagine)
    // 📌 Modifica una stagione esistente (supporta cambio immagine e aggiornamento parziale)
    public StagioneRespDTO updateStagione(UUID id, String titolo, String anno, MultipartFile immagine) {
        Stagione stagione = stagioneRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stagione non trovata con ID: " + id));

        // 🔹 Aggiorna solo i campi forniti (se non null o vuoti)
        if (titolo != null && !titolo.isEmpty()) {
            stagione.setTitolo(titolo);
        }
        if (anno != null && !anno.isEmpty()) {
            stagione.setAnno(anno);
        }

        // 🔹 Se viene fornita una nuova immagine, sostituisci quella esistente
        if (immagine != null && !immagine.isEmpty()) {
            String imageUrl = uploadImageToCloudinary(immagine);
            stagione.setImmagineUrl(imageUrl);
        }

        return convertToDTO(stagioneRepository.save(stagione));
    }

    // 📌 Cancella una stagione e i relativi video
    public boolean deleteStagione(UUID id) {
        if (!stagioneRepository.existsById(id)) {
            throw new NotFoundException("Stagione non trovata con ID: " + id);
        }
        try {
            Stagione stagione = stagioneRepository.findById(id).orElse(null);

            if (stagione != null) {
                List<Video> videoList = videoRepository.findByStagioneId(id);
                for (Video video : videoList) {
                    videoService.deleteVideo(video.getId());
                }
            }

            stagioneRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante la cancellazione della stagione.");
        }
    }

    // 📌 Metodo per convertire una Stagione in StagioneRespDTO
    private StagioneRespDTO convertToDTO(Stagione stagione) {
        return new StagioneRespDTO(
                stagione.getId(),
                stagione.getTitolo(),
                stagione.getAnno(),
                stagione.getImmagineUrl(),
                stagione.getSezione().getTitolo(),
                stagione.getSezione().getId(),
                stagione.getVideoList() != null
                        ? stagione.getVideoList().stream().map(Video::getTitolo).collect(Collectors.toList())
                        : new ArrayList<>() // 🔥 Previene NullPointerException
        );
    }


    // 📌 Metodo per gestire l'upload su Cloudinary
    private String uploadImageToCloudinary(MultipartFile file) {
        try {
            Cloudinary cloudinary = getCloudinaryInstance();
            return cloudinary.uploader()
                    .upload(file.getBytes(), ObjectUtils.emptyMap())
                    .get("url")
                    .toString();
        } catch (IOException e) {
            throw new InternalServerErrorException("Errore durante il caricamento dell'immagine su Cloudinary.");
        }
    }
}
