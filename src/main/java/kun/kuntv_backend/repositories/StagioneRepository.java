package kun.kuntv_backend.repositories;

import kun.kuntv_backend.entities.Stagione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StagioneRepository extends JpaRepository<Stagione, UUID> {
    // Metodo per trovare stagioni per una sezione
    List<Stagione> findBySezioneId(UUID sezioneId);

    // Trova stagioni che contengono una certa stringa nel titolo
    List<Stagione> findByTitoloContainingIgnoreCase(String titolo);

    // Trova stagioni per una specifica sezione
    List<Stagione> findBySezione_TitoloContainingIgnoreCase(String sezioneTitolo);

    // Trova stagioni per anno
    List<Stagione> findByAnnoContainingIgnoreCase(String anno);
}
