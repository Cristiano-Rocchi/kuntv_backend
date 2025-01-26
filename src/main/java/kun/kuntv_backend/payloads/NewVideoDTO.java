package kun.kuntv_backend.payloads;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class NewVideoDTO {
    private String titolo;
    private String durata;
    private UUID stagioneId;
}

