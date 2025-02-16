package kun.kuntv_backend.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import kun.kuntv_backend.enums.TagSezione;
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

    @Column(nullable = false, unique = true)
    private String titolo;

    @Column(nullable = false)
    private String fotoUrl;

    @Column(nullable = false)
    private String anno;

    @ElementCollection(targetClass = TagSezione.class)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private List<TagSezione> tag;



    @OneToMany(mappedBy = "sezione")
    @JsonManagedReference("sezione-video")
    private List<Video> videoList;

    @OneToMany(mappedBy = "sezione")
    @JsonManagedReference("sezione-stagioni")
    private List<Stagione> stagioni;

    @ManyToOne
    @JoinColumn(name = "collection_id", nullable = false)
    @JsonBackReference("collection-sezioni")
    private Collection collection;
}

