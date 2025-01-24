package kun.kuntv_backend.controller;

import kun.kuntv_backend.entities.Film;
import kun.kuntv_backend.enums.CollectionType;
import kun.kuntv_backend.services.FilmService;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/films")
public class FilmController {

    @Autowired
    private FilmService filmService;

    // Creazione di un nuovo film con caricamento su Cloudinary (solo admin)
    @PostMapping("/upload")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Film> createFilm(
            @RequestParam CollectionType tipo,
            @RequestParam("file") MultipartFile file,
            @RequestParam String titolo,
            @RequestParam String genere,
            @RequestParam String durata) {

        try {
            // Creiamo un oggetto Film usando i parametri forniti
            Film film = new Film();
            film.setTitolo(titolo);
            film.setGenere(genere);
            film.setDurata(durata);

            // Chiamiamo il servizio per creare il film
            Film createdFilm = filmService.createFilm(film, tipo, file);
            return ResponseEntity.status(201).body(createdFilm);
        } catch (InternalServerErrorException e) {
            return ResponseEntity.status(500).body(null); // Errore durante il caricamento
        }
    }

    // Visualizzazione di tutti i film (user e admin)
    @GetMapping
    public ResponseEntity<List<Film>> getAllFilms() {
        return ResponseEntity.ok(filmService.getAllFilms());
    }

    // Visualizzazione di un film per ID (user e admin)
    @GetMapping("/{id}")
    public ResponseEntity<Film> getFilmById(@PathVariable Long id) {
        Optional<Film> film = filmService.getFilmById(id);
        return film.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(null));
    }

    // Cancellazione di un film (solo admin)
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFilm(@PathVariable Long id) {
        try {
            filmService.deleteFilm(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).build();
        }
    }
}

