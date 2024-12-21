package kun.kuntv_backend.payloads;

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
    private String tag;
    private String anno;
    private List<String> stagioniTitoli; // Lista dei titoli delle stagioni
    private List<String> videoTitoli;



}
