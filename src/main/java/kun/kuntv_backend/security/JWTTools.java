package kun.kuntv_backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JWTTools {

    @Value("${jwt.secret}")
    private String secret;  // La chiave segreta per firmare il token

    public String createToken(String role) {
        return Jwts.builder()
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7)) // Scadenza di 7 giorni
                .setSubject(role) // Il subject sarà il ruolo (user/admin)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes())) // Firma con la chiave segreta
                .compact();
    }

    public void verifyToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))  // Imposta la chiave segreta
                    .build()  // Costruisce il parser
                    .parseClaimsJws(token);  // Verifica il token
        } catch (Exception ex) {
            throw new RuntimeException("Token invalido o scaduto. Effettua di nuovo il login.");
        }
    }

    public String extractRoleFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))  // Imposta la chiave segreta
                .build()  // Costruisce il parser
                .parseClaimsJws(token)  // Decodifica il token
                .getBody()  // Ottieni il corpo del token
                .getSubject();  // Restituisci il subject (il ruolo, user/admin)
    }
}
