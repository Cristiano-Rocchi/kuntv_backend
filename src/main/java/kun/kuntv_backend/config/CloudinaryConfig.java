package kun.kuntv_backend.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public List<Cloudinary> cloudinaryAccounts(
            @Value("${cloudinary.name.account1}") String name1,
            @Value("${cloudinary.key.account1}") String key1,
            @Value("${cloudinary.secret.account1}") String secret1,
            @Value("${cloudinary.name.account2}") String name2,
            @Value("${cloudinary.key.account2}") String key2,
            @Value("${cloudinary.secret.account2}") String secret2) {

        List<Cloudinary> accounts = new ArrayList<>();

        Map<String, String> config1 = new HashMap<>();
        config1.put("cloud_name", name1);
        config1.put("api_key", key1);
        config1.put("api_secret", secret1);
        accounts.add(new Cloudinary(config1));

        Map<String, String> config2 = new HashMap<>();
        config2.put("cloud_name", name2);
        config2.put("api_key", key2);
        config2.put("api_secret", secret2);
        accounts.add(new Cloudinary(config2));

        return accounts;
    }
}
