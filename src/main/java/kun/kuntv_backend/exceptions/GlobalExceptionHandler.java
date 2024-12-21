package kun.kuntv_backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice  // Gestisce tutte le eccezioni globalmente
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)  // Intercetta UnauthorizedException
    public ResponseEntity<String> handleUnauthorizedException(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());  // Restituisce una risposta 401 con il messaggio
    }

    @ExceptionHandler(InternalServerErrorException.class)  // Gestione dell'errore 500
    public ResponseEntity<String> handleInternalServerErrorException(InternalServerErrorException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());  // Risposta 500 con il messaggio dell'errore
    }


    @ExceptionHandler(NotFoundException.class)  // Gestisce NotFoundException
    public ResponseEntity<String> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());  // Risposta 404 con il messaggio dell'errore
    }

    @ExceptionHandler(BadRequestException.class)  // Gestisce BadRequestException
    public ResponseEntity<String> handleBadRequestException(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());  // Risposta 400 con il messaggio dell'errore
    }

    @ExceptionHandler(MethodNotAllowedException.class)  // Gestisce MethodNotAllowedException
    public ResponseEntity<String> handleMethodNotAllowedException(MethodNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ex.getMessage());  // Risposta 405 con il messaggio dell'errore
    }

    @ExceptionHandler(InvalidSecretException.class)
    public ResponseEntity<String> handleInvalidSecret(InvalidSecretException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    // Gestisce gli errori di endpoint non trovato (404)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<String> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        // Restituisce un messaggio personalizzato per l'errore 404
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Sintassi dell'endpoint sbagliata o endpoint non trovato.");
    }

}
