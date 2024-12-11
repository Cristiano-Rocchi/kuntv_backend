package kun.kuntv_backend.config;

import org.springframework.stereotype.Component;

@Component
public class SecretManager {

    private final String userSecret;
    private final String adminSecret;

    // Costruttore che inizializza i segreti
    public SecretManager(String userSecret, String adminSecret) {
        this.userSecret = userSecret;
        this.adminSecret = adminSecret;
        System.out.println("SecretManager inizializzato");
        System.out.println("User secret: " + this.userSecret);
        System.out.println("Admin secret: " + this.adminSecret);
    }

    // Verifica il segreto immesso e restituisce il ruolo
    public String verifySecret(String inputSecret) {
        System.out.println("Verificando il segreto: " + inputSecret); // Log per debugging

        // Controlla se inputSecret è null
        if (inputSecret == null) {
            return "invalid"; // Ritorna "invalid" se il segreto è null
        }

        // Confronta i segreti solo se inputSecret non è null
        if (inputSecret.equals(userSecret)) {
            return "user"; // Restituisce "user" se il segreto corrisponde
        } else if (inputSecret.equals(adminSecret)) {
            return "admin"; // Restituisce "admin" se il segreto corrisponde
        } else {
            return "invalid"; // Restituisce "invalid" se il segreto non corrisponde
        }
    }


    // Verifica se il segreto corrisponde a quello dell'admin
    public boolean isAdmin(String token) {
        System.out.println("Verifica del segreto per il token: " + token); // Log per debugging
        return token.equals(adminSecret);  // Assicurati che il confronto funzioni correttamente
    }


    // Verifica se il segreto corrisponde a quello dell'utente
    public boolean isUser(String inputSecret) {
        return inputSecret.equals(userSecret); // Verifica se il segreto è quello dell'utente
    }
}
