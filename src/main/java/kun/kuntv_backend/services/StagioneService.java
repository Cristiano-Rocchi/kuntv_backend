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
    @Autowired
    private SezioneRepository sezioneRepository;
    @Autowired
    private VideoService videoService;
    @Autowired
    private List<Cloudinary> cloudinaryAccounts;


    // Ottieni tutte le stagioni
    public List<StagioneRespDTO> getAllStagioni() {
        List<Stagione> stagioni = stagioneRepository.findAll();
        return stagioni.stream()
                .map(stagione -> new StagioneRespDTO(
                        stagione.getId(),
                        stagione.getTitolo(),
                        stagione.getAnno(),
                        stagione.getImmagineUrl(),
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
    // Metodo per scegliere un account Cloudinary in modo bilanciato
    private Cloudinary getCloudinaryInstance() {
        return cloudinaryAccounts.get((int) (Math.random() * cloudinaryAccounts.size()));
    }

    // Crea una nuova stagione (supporta upload immagine opzionale)
    public Stagione createStagione(NewStagioneDTO dto, MultipartFile immagine) {
        Sezione sezione = sezioneRepository.findById(dto.getSezioneId())
                .orElseThrow(() -> new NotFoundException("Sezione non trovata con ID: " + dto.getSezioneId()));

        Stagione stagione = new Stagione();
        stagione.setTitolo(dto.getTitolo());
        stagione.setAnno(dto.getAnno());
        stagione.setSezione(sezione);

        if (immagine != null && !immagine.isEmpty()) {
            try {
                Cloudinary cloudinary = getCloudinaryInstance();
                String imageUrl = cloudinary.uploader().upload(immagine.getBytes(), ObjectUtils.emptyMap()).get("url").toString();
                stagione.setImmagineUrl(imageUrl);
            } catch (IOException e) {
                throw new InternalServerErrorException("Errore durante il caricamento dell'immagine su Cloudinary.");
            }
        }

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
                // Recupera i video associati alla stagione
                List<Video> videoList = videoRepository.findByStagioneId(id);
                for (Video video : videoList) {
                    // Usa il metodo del VideoService per eliminare il video dal db e da Backblaze
                    videoService.deleteVideo(video.getId());
                }
            }

            // Infine, elimina la stagione
            stagioneRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante la cancellazione della stagione.");
        }
    }
}
