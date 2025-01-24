package kun.kuntv_backend.services;


import kun.kuntv_backend.entities.Collection;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.payloads.CollectionRespDTO;
import kun.kuntv_backend.repositories.CollectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CollectionService {

    @Autowired
    private CollectionRepository collectionRepository;

    public CollectionRespDTO getCollectionDetails(UUID id) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Collection non trovata con ID: " + id));

        return new CollectionRespDTO(
                collection.getId(),
                collection.getTipo().name(),
                collection.getFilm(),
                collection.getSezioni()
        );
    }
}
