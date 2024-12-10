package kun.kuntv_backend.config;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecretManagerConfig {

    // Parola segreta per l'utente (lettura da application.properties o env.properties)
    @Value("${USER_SECRET}")
    private String userSecret;

    // Parola segreta per l'admin (lettura da application.properties o env.properties)
    @Value("${ADMIN_SECRET}")
    private String adminSecret;

    // Crea e restituisce il SecretManager
    @Bean
    public SecretManager secretManager() {
        return new SecretManager(userSecret, adminSecret);
    }
}
