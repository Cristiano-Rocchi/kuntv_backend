package kun.kuntv_backend.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @Column(nullable = false, length = 1000)
    private String fileLink;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCaricamento = LocalDateTime.now();
    @ManyToOne
    @JoinColumn(name = "sezione_id", nullable = false)
    @JsonBackReference("sezione-video")
    private Sezione sezione;

    @ManyToOne
    @JoinColumn(name = "stagione_id")
    @JsonBackReference("stagione-video")
    private Stagione stagione;


}

