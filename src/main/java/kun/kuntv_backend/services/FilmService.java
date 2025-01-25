package kun.kuntv_backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import kun.kuntv_backend.entities.Collection;
import kun.kuntv_backend.entities.Film;
import kun.kuntv_backend.enums.CollectionType;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.repositories.CollectionRepository;
import kun.kuntv_backend.repositories.FilmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class FilmService {

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private List<Cloudinary> cloudinaryAccounts;

    public Film createFilm(Film film, CollectionType tipo, MultipartFile file) {
        Collection collection = collectionRepository.findByTipo(tipo)
                .orElseGet(() -> {
                    Collection newCollection = new Collection();
                    newCollection.setTipo(tipo);
                    return collectionRepository.save(newCollection);
                });

        film.setCollection(collection);

        Exception lastException = null;
        for (Cloudinary cloudinary : cloudinaryAccounts) {
            try {
                // Carica il file sul primo account disponibile
                Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                        ObjectUtils.asMap("resource_type", "video"));

                // Ottieni il link del file caricato
                String fileLink = (String) uploadResult.get("secure_url");
                film.setVideoUrl(fileLink);

                return filmRepository.save(film);
            } catch (IOException e) {
                lastException = e; // Prova con il prossimo account
            }
        }

        throw new InternalServerErrorException("Errore durante il caricamento del film su Cloudinary: " +
                (lastException != null ? lastException.getMessage() : "Nessun account disponibile"));
    }

    public Optional<Film> getFilmById(Long id) {
        return filmRepository.findById(id);
    }

    public List<Film> getAllFilms() {
        return filmRepository.findAll();
    }

    public Film updateFilm(Film film) {
        return filmRepository.save(film);
    }

    public void deleteFilm(Long id) {
        if (!filmRepository.existsById(id)) {
            throw new NotFoundException("Film not found with ID: " + id);
        }
        filmRepository.deleteById(id);
    }
}
