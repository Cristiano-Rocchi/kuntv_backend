package kun.kuntv_backend.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class VideoRespDTO {
    private UUID id;
    private String titolo;
    private String durata;
    private String fileLink;
    private String stagioneTitolo;
    private String sezioneTitolo;
    private String bucketName;
}

