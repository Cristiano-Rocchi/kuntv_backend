package kun.kuntv_backend.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import kun.kuntv_backend.enums.CollectionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Collection {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    private CollectionType tipo;

    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("collection-film")
    private List<Film> film;

    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("collection-sezioni")
    private List<Sezione> sezioni;
}
