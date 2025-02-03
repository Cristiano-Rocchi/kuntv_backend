package kun.kuntv_backend.payloads;
import kun.kuntv_backend.enums.TagSezione;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class NewSezioneDTO {
    private String titolo;
    private String anno;
    private MultipartFile file;

    private String tag; // 🔥 Ora `tag` è una stringa (es. "DOCUMENTARIO,DRAMMATICO")

    // Metodo per convertire `tag` da stringa CSV a List<TagSezione>
    public List<TagSezione> getTagAsEnumList() {
        if (tag == null || tag.isEmpty()) return List.of();
        return Arrays.stream(tag.split(",")) // Divide per virgola
                .map(String::trim) // 🔥 Rimuove eventuali spazi
                .map(TagSezione::valueOf) // 🔥 Converte in enum
                .collect(Collectors.toList());
    }
}
