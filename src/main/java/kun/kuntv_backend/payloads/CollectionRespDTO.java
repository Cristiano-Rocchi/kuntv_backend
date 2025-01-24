package kun.kuntv_backend.payloads;

import kun.kuntv_backend.entities.Film;
import kun.kuntv_backend.entities.Sezione;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class CollectionRespDTO {
    private UUID id;
    private String tipo; // Tipo della Collection (FILM o SERIE_TV)
    private List<Film> film; // Lista dei film associati
    private List<Sezione> sezioni; // Lista delle sezioni associate
}

