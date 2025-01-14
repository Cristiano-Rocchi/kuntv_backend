package kun.kuntv_backend.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.model.File;
import com.google.api.client.http.FileContent;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

@Service
public class GoogleDriveService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveService.class);

    /**
     * Carica un file su Google Drive utilizzando il `Credential`.
     */
    public String uploadFile(Path filePath, String fileName, String mimeType, Credential credential) throws IOException, GeneralSecurityException {
        if (credential == null || credential.getAccessToken() == null) {
            throw new IllegalStateException("Le credenziali non sono valide. Autenticati prima di caricare il video.");
        }

        // Crea il servizio Google Drive con le credenziali
        Drive driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("Kuntv")
                .build();

        // Crea i metadati del file
        File fileMetadata = new File();
        fileMetadata.setName(fileName);

        // Contenuto del file
        FileContent mediaContent = new FileContent(mimeType, filePath.toFile());

        try {
            logger.info("Inizio caricamento del file '{}' su Google Drive...", fileName);
            File file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();

            logger.info("Caricamento completato con successo! ID del file: {}", file.getId());
            return file.getId();
        } catch (IOException e) {
            logger.error("Errore durante l'upload del file '{}'", fileName, e);
            throw e;
        }
    }
}
