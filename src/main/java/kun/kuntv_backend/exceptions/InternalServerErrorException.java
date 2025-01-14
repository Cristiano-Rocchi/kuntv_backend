package kun.kuntv_backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)  // Mappa l'eccezione a HTTP 500 Internal Server Error
public class InternalServerErrorException extends RuntimeException {
    public InternalServerErrorException(String message) {
        super(message);
    }

    // Costruttore che accetta anche la causa
    public InternalServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
