package kun.kuntv_backend.config;


import org.springframework.stereotype.Component;

@Component
public class SecretManager {

    private final String userSecret;
    private final String adminSecret;

    // Costruttore per inizializzare le parole segrete
    public SecretManager(String userSecret, String adminSecret) {
        this.userSecret = userSecret;
        this.adminSecret = adminSecret;
    }

    // Metodo per verificare la parola segreta inserita
    public String verifySecret(String inputSecret) {
        if (inputSecret.equals(userSecret)) {
            return "user";  // Restituisce "user" se la parola segreta corrisponde a quella dell'utente
        } else if (inputSecret.equals(adminSecret)) {
            return "admin";  // Restituisce "admin" se la parola segreta corrisponde a quella dell'admin
        } else {
            return "invalid";  // Restituisce "invalid" se la parola segreta non è valida
        }
    }
}

