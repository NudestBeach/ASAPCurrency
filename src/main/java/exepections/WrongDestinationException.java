package exepections;

import net.sharksystem.asap.ASAPException;

public class WrongDestinationException extends ASAPException {
    public WrongDestinationException(String message) {
        super(message);
    }
}
