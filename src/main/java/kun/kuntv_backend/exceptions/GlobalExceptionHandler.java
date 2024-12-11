package kun.kuntv_backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice  // Gestisce tutte le eccezioni globalmente
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)  // Intercetta UnauthorizedException
    public ResponseEntity<String> handleUnauthorizedException(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());  // Restituisce una risposta 401 con il messaggio
    }
}
