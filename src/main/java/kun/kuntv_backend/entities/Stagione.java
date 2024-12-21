package kun.kuntv_backend.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    @JsonBackReference("sezione-stagioni")
    private Sezione sezione;

    @OneToMany(mappedBy = "stagione")
    @JsonManagedReference("stagione-video")
    private List<Video> videoList;
}
