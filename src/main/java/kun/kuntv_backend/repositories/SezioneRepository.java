package kun.kuntv_backend.repositories;

import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.enums.TagSezione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SezioneRepository extends JpaRepository<Sezione, UUID> {

    // Cerca sezioni con un singolo tag
    @Query("SELECT s FROM Sezione s WHERE :tag MEMBER OF s.tag")
    List<Sezione> findBySingleTag(@Param("tag") TagSezione tag);

    // Cerca sezioni che contengono TUTTI i tag selezionati
    @Query("SELECT s FROM Sezione s WHERE s.id IN (SELECT s2.id FROM Sezione s2 JOIN s2.tag t WHERE t IN :tags GROUP BY s2.id HAVING COUNT(DISTINCT t) = :size)")
    List<Sezione> findByMultipleTags(@Param("tags") List<TagSezione> tags, @Param("size") long size);

    // Cerca sezioni per titolo (ignorando maiuscole e minuscole)
    List<Sezione> findByTitoloContainingIgnoreCase(String titolo);
    Optional<Sezione> findByTitoloIgnoreCase(String titolo);


    // Cerca sezioni per anno
    List<Sezione> findByAnno(String anno);
}
