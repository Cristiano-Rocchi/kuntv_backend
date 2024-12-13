package kun.kuntv_backend.services;

import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.repositories.VideoRepository;
import kun.kuntv_backend.repositories.StagioneRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final StagioneRepository stagioneRepository;

    public VideoService(VideoRepository videoRepository, StagioneRepository stagioneRepository) {
        this.videoRepository = videoRepository;
        this.stagioneRepository = stagioneRepository;
    }

    // Ottieni tutti i video
    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

    // Ottieni video per ID
    public Optional<Video> getVideoById(UUID id) {
        return videoRepository.findById(id);
    }

    // Ottieni video per sezione
    public List<Video> getVideosBySezioneId(UUID sezioneId) {
        return videoRepository.findBySezioneId(sezioneId);
    }

    // Ottieni video per stagione (opzionale)
    public List<Video> getVideosByStagioneId(UUID stagioneId) {
        return videoRepository.findByStagioneId(stagioneId);
    }

    // Crea un nuovo video (solo admin)
    public Video createVideo(UUID stagioneId, Video video) {
        // Recupera la stagione usando l'ID fornito
        Optional<Stagione> stagioneOpt = stagioneRepository.findById(stagioneId);
        if (stagioneOpt.isPresent()) {
            Stagione stagione = stagioneOpt.get();
            Sezione sezione = stagione.getSezione(); // Ottieni la sezione associata alla stagione

            // Imposta la stagione e la sezione sul video
            video.setStagione(stagione);
            video.setSezione(sezione); // Associa la sezione al video

            // Aggiungi il video alla lista della stagione
            stagione.getVideoList().add(video); // Aggiungi il video alla lista di video della stagione

            // Salva la stagione per assicurarti che la lista venga aggiornata nel database
            stagioneRepository.save(stagione); // Salva la stagione aggiornata

            // Salva il video nel database
            return videoRepository.save(video);
        } else {
            // Gestisci l'errore nel caso in cui la stagione non esista
            throw new IllegalArgumentException("Stagione non trovata con l'ID: " + stagioneId);
        }
    }



    // Modifica un video esistente (solo admin)
    public Optional<Video> updateVideo(UUID id, Video updatedVideo) {
        return videoRepository.findById(id).map(video -> {
            // Aggiorna i dettagli del video
            video.setTitolo(updatedVideo.getTitolo());
            video.setDurata(updatedVideo.getDurata());
            video.setFileLink(updatedVideo.getFileLink());

            // Se è stata fornita una nuova stagione, aggiorna anche la sezione
            if (updatedVideo.getStagione() != null) {
                Optional<Stagione> stagioneOpt = stagioneRepository.findById(updatedVideo.getStagione().getId());
                if (stagioneOpt.isPresent()) {
                    Stagione nuovaStagione = stagioneOpt.get();
                    video.setStagione(nuovaStagione);
                    video.setSezione(nuovaStagione.getSezione()); // Associa automaticamente la sezione della stagione
                } else {
                    throw new IllegalArgumentException("Stagione non trovata con ID: " + updatedVideo.getStagione().getId());
                }
            }

            // Salva il video aggiornato
            return videoRepository.save(video);
        });
    }


    // Cancella un video (solo admin)
    public boolean deleteVideo(UUID id) {
        if (videoRepository.existsById(id)) {
            videoRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
