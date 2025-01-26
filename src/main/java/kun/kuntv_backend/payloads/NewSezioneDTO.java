package kun.kuntv_backend.payloads;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class NewSezioneDTO {
    private String titolo;
    private String tag;
    private String anno;
    private MultipartFile file;
}
