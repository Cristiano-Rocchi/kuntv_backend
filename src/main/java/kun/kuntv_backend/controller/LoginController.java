package kun.kuntv_backend.controller;

import kun.kuntv_backend.config.SecretManager;
import kun.kuntv_backend.security.JWTTools;
import kun.kuntv_backend.exceptions.InvalidSecretException;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final SecretManager secretManager;
    private final JWTTools jwtTools;

    public LoginController(SecretManager secretManager, JWTTools jwtTools) {
        this.secretManager = secretManager;
        this.jwtTools = jwtTools;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody SecretRequest secretRequest) {
        try {
            if (secretRequest == null || secretRequest.getSecret() == null || secretRequest.getSecret().isEmpty()) {
                throw new InvalidSecretException("Segreto mancante o vuoto");
            }

            String role = secretManager.verifySecret(secretRequest.getSecret());
            if ("invalid".equals(role)) {
                throw new InvalidSecretException("Segreto non valido");
            }

            // Crea il token JWT con il ruolo
            String token = jwtTools.createToken(role);

            // Restituisci il token JWT
            return ResponseEntity.ok(token);
        } catch (InvalidSecretException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            throw new InternalServerErrorException("Errore durante il processo di login");
        }
    }

    public static class SecretRequest {
        private String secret;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}
