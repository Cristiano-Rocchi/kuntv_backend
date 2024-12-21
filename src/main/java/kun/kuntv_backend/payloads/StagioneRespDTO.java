package kun.kuntv_backend.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;

import lombok.Setter;

import java.util.List;
import java.util.UUID;
@Getter
@Setter
@AllArgsConstructor
public class StagioneRespDTO {
    private UUID id;
    private String titolo;
    private String anno;
    private String sezioneTitolo;    // Titolo della sezione associata
    private List<String> videoTitoli; }
