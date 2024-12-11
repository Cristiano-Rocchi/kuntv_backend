package kun.kuntv_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
                .csrf(csrf -> csrf.disable())  // Disabilita CSRF se non necessario
                .authorizeHttpRequests(auth -> auth
                        // Permetti accesso a /auth/login senza autenticazione
                        .requestMatchers("/auth/login").permitAll()

                        // Permetti GET per /api/sezioni e /api/sezioni/{id} a tutti (user e admin)
                        .requestMatchers(HttpMethod.GET, "/api/sezioni", "/api/sezioni/**").permitAll()

                        // Permetti solo agli admin di fare operazioni CRUD su /api/sezioni (POST, PUT, DELETE)
                        .requestMatchers(HttpMethod.POST, "/api/sezioni").hasRole("admin")
                        .requestMatchers(HttpMethod.PUT, "/api/sezioni/**").hasRole("admin")
                        .requestMatchers(HttpMethod.DELETE, "/api/sezioni/**").hasRole("admin")

                        // Le altre richieste necessitano di autenticazione
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // Gestione stateless
                .addFilterBefore(jwtCheckFilter, UsernamePasswordAuthenticationFilter.class); // Aggiungi il filtro JWT prima del filtro di autenticazione
        return httpSecurity.build();
    }
}
