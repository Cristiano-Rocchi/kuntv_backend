package kun.kuntv_backend.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "video")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    @GeneratedValue
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(nullable = false)
    private String titolo;


    @Column(nullable = false)
    private String durata;

    @Column(nullable = false)
    private String fileLink;

    @ManyToOne
    @JoinColumn(name = "sezione_id", nullable = false)
    @JsonBackReference("sezione-video")
    private Sezione sezione;

    @ManyToOne
    @JoinColumn(name = "stagione_id")
    @JsonBackReference("stagione-video")
    private Stagione stagione;
}

