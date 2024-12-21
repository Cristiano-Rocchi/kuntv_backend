package kun.kuntv_backend.exceptions;

public class InvalidSecretException extends RuntimeException {
    public InvalidSecretException(String message) {
        super(message);
    }
}
