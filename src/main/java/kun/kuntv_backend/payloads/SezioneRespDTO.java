package kun.kuntv_backend.payloads;

import kun.kuntv_backend.enums.TagSezione;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


import java.util.List;
import java.util.UUID;
@Getter
@Setter
@AllArgsConstructor
public class SezioneRespDTO {
    private UUID id;
    private String titolo;
    private String foto;
    private List<TagSezione> tag;
    private String anno;
    private List<String> stagioniTitoli; // Lista dei titoli delle stagioni
    private List<String> videoTitoli;
    private UUID collectionId; // ID della Collection
    private String collectionTipo; // Tipo della Collection (FILM o SERIE_TV)
}
