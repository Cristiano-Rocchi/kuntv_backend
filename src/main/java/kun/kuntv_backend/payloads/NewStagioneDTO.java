package kun.kuntv_backend.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class NewStagioneDTO {
    private String titolo;
    private String anno;
    private UUID sezioneId;
    private MultipartFile file; // URL dell'immagine (opzionale)
}

