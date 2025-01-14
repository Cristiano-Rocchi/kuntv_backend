package kun.kuntv_backend.controller;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {

    private final GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow;
    private static Credential currentCredential;  // Memorizza le credenziali dell'utente

    @Autowired
    public GoogleAuthController(GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow) {
        this.googleAuthorizationCodeFlow = googleAuthorizationCodeFlow;
    }

    @GetMapping("/authorize")
    public ResponseEntity<String> startGoogleAuth() throws Exception {
        String authUrl = googleAuthorizationCodeFlow.newAuthorizationUrl()
                .setRedirectUri("http://localhost:3001/callback")
                .build();
        return ResponseEntity.ok("Autorizza l'app visitando questo link: " + authUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleGoogleCallback(@RequestParam("code") String code) {
        try {
            GoogleTokenResponse tokenResponse = googleAuthorizationCodeFlow.newTokenRequest(code)
                    .setRedirectUri("http://localhost:3001/callback")
                    .execute();

            currentCredential = googleAuthorizationCodeFlow.createAndStoreCredential(tokenResponse, "user");
            return ResponseEntity.ok("Autenticazione completata con successo!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Errore durante la callback di Google: " + e.getMessage());
        }
    }

    public static Credential getCredential() {
        return currentCredential;
    }
}
