package kun.kuntv_backend.repositories;




import kun.kuntv_backend.entities.Sezione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SezioneRepository extends JpaRepository<Sezione, UUID> {

}

