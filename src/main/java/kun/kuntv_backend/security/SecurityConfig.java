package kun.kuntv_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JWTCheckFilter jwtCheckFilter;

    // Inietta il filtro JWTCheckFilter
    public SecurityConfig(JWTCheckFilter jwtCheckFilter) {
        this.jwtCheckFilter = jwtCheckFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Aggiungi questa linea
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/sezioni", "/api/sezioni/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/sezioni", "/api/stagioni", "/api/video").hasRole("admin")
                        .requestMatchers(HttpMethod.PUT, "/api/sezioni/**", "/api/stagioni/**", "/api/video/**").hasRole("admin")
                        .requestMatchers(HttpMethod.DELETE, "/api/sezioni/**", "/api/stagioni/**", "/api/video/**").hasRole("admin")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtCheckFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000")); // Modifica con i domini del tuo frontend
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Metodi HTTP permessi
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type")); // Intestazioni permesse
        configuration.setExposedHeaders(List.of("Authorization")); // Intestazioni visibili
        configuration.setAllowCredentials(true); // Permetti invio di credenziali (es. cookie)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Applica CORS a tutte le rotte
        return source;
    }
}
