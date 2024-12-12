package kun.kuntv_backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stagione")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Stagione {

    @Id
    @GeneratedValue
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(nullable = false)
    private String titolo;

    @Column(nullable = false)
    private String anno;

    @ManyToOne
    @JoinColumn(name = "sezione_id", nullable = false)
    private Sezione sezione;

    @OneToMany(mappedBy = "stagione")
    private List<Video> videoList;
}
