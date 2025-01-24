package kun.kuntv_backend.repositories;

import kun.kuntv_backend.entities.Collection;
import kun.kuntv_backend.enums.CollectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, UUID> {
    Optional<Collection> findByTipo(CollectionType tipo);
}
