package kun.kuntv_backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sezione")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sezione {

    @Id
    @GeneratedValue
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(nullable = false)
    private String titolo;

    @Column(nullable = false)
    private String fotoUrl;

    @OneToMany(mappedBy = "sezione")
    private List<Video> videoList;

    @OneToMany(mappedBy = "sezione")
    private List<Stagione> stagioni;
}

