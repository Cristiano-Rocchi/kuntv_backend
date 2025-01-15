package kun.kuntv_backend.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Configuration
public class GoogleDriveConfig {

    private static final String APPLICATION_NAME = "Kuntv";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);

    // Inject variabili d'ambiente dal file `application.properties` / `env.properties`
    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    @Value("${google.auth.uri}")
    private String authUri;

    @Value("${google.token.uri}")
    private String tokenUri;

    @Value("${google.cert.url}")
    private String certUrl;

    @Value("${google.project.id}")
    private String projectId;

    @Value("${google.javascript.origin}")
    private String javascriptOrigin;

    @Bean
    public GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow() throws GeneralSecurityException, IOException {
        // Costruzione del JSON simulando il file delle credenziali
        String jsonCredentials = String.format("""
                {
                  "web": {
                    "client_id": "%s",
                    "project_id": "%s",
                    "auth_uri": "%s",
                    "token_uri": "%s",
                    "auth_provider_x509_cert_url": "%s",
                    "client_secret": "%s",
                    "redirect_uris": ["%s"],
                    "javascript_origins": ["%s"]
                  }
                }
                """, clientId, projectId, authUri, tokenUri, certUrl, clientSecret, redirectUri, javascriptOrigin);

        // Carica i client secrets a partire dalla stringa JSON
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new StringReader(jsonCredentials));

        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets, SCOPES)
                .setAccessType("offline")
                .setApprovalPrompt("force") // Richiede autorizzazione ogni volta
                .build();
    }
}
