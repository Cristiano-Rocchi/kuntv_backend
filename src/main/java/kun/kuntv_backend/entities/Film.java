package kun.kuntv_backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Film {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String Titolo;
    private String Genere;
    private String Durata;
    private String VideoUrl;

    @ManyToOne
    @JoinColumn(name = "collection_id")
    private Collection collection;

}
