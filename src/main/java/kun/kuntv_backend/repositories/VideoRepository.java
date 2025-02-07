package kun.kuntv_backend.repositories;

import kun.kuntv_backend.entities.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    // Metodo per trovare video per una sezione
    List<Video> findBySezioneId(UUID sezioneId);

    // Metodo per trovare video per una stagione
    List<Video> findByStagioneId(UUID stagioneId);

    // 🔹 Metodi per la ricerca avanzata
    List<Video> findByTitoloContainingIgnoreCase(String titolo);
    List<Video> findBySezione_TitoloContainingIgnoreCase(String sezioneTitolo);
    List<Video> findByStagione_TitoloContainingIgnoreCase(String stagioneTitolo);
    List<Video> findByFileLinkContainingIgnoreCase(String bucketName);
}
