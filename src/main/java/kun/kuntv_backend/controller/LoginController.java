package kun.kuntv_backend.controller;

import kun.kuntv_backend.config.SecretManager;
import kun.kuntv_backend.security.JWTTools;
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

        String role = secretManager.verifySecret(secretRequest.getSecret());
        if ("invalid".equals(role)) {
            return ResponseEntity.status(403).body("Invalid secret");
        }

        // Crea il token JWT con il ruolo
        String token = jwtTools.createToken(role);

        // Restituisci il token JWT
        return ResponseEntity.ok(token);
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
