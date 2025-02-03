package kun.kuntv_backend.payloads;

import kun.kuntv_backend.enums.TagSezione;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class NewSezioneDTO {
    private String titolo;
    private List<String> tag; // Cambiato a List<String> per ricevere dati JSON correttamente
    private String anno;
    private MultipartFile file;

    // Metodo per convertire i tag in enum
    public List<TagSezione> getTagAsEnumList() {
        return tag.stream()
                .map(TagSezione::valueOf) // Converte stringa in enum
                .collect(Collectors.toList());
    }
}
