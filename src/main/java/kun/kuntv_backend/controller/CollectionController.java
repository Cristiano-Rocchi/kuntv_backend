package kun.kuntv_backend.controller;

import kun.kuntv_backend.payloads.CollectionRespDTO;
import kun.kuntv_backend.services.CollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    @GetMapping("/{id}")
    public ResponseEntity<CollectionRespDTO> getCollectionDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(collectionService.getCollectionDetails(id));
    }
}