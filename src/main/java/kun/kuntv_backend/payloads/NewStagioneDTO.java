package kun.kuntv_backend.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NewStagioneDTO {
    private String titolo;
    private String anno;
    private UUID sezioneId;
    private MultipartFile immagine; // Il file ora viene ricevuto direttamente
}
