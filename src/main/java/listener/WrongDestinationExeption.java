package listener;

public class WrongDestinationExeption extends RuntimeException {
    public WrongDestinationExeption(String message) {
        super(message);
    }
}
