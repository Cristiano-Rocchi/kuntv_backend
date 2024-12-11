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
}
