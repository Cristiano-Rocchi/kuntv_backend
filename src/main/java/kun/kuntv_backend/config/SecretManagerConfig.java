package kun.kuntv_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import kun.kuntv_backend.config.SecretManager;

@Configuration
public class SecretManagerConfig {

    // Parola segreta per l'utente
    @Value("${user.secret}")
    private String userSecret;

    // Parola segreta per l'admin
    @Value("${admin.secret}")
    private String adminSecret;

    // Crea e restituisce il SecretManager
    @Bean
    public SecretManager secretManager() {
        return new SecretManager(userSecret, adminSecret); // Passa solo i segreti
    }
}
