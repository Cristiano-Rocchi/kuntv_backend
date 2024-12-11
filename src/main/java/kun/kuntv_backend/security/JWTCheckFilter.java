package kun.kuntv_backend.security;

import kun.kuntv_backend.exceptions.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class JWTCheckFilter extends OncePerRequestFilter {

    private final JWTTools jwtTools;

    @Autowired
    public JWTCheckFilter(JWTTools jwtTools) {
        this.jwtTools = jwtTools;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Per favore inserisci correttamente il token nell'Authorization Header");
        }

        String accessToken = authHeader.substring(7);

        jwtTools.verifyToken(accessToken); // Verifica la validità del token
        String role = jwtTools.extractRoleFromToken(accessToken); // Estrai il ruolo dal token

        // Crea un'istanza di GrantedAuthority per il ruolo estratto
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                null, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );

        // Imposta l'autenticazione nel contesto di sicurezza di Spring
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Non applicare il filtro alle rotte di autenticazione
        return new AntPathMatcher().match("/auth/**", request.getServletPath());
    }
}
