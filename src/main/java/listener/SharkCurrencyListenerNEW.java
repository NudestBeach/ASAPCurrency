package listener;

import exepections.SharkPromiseException;
import net.sharksystem.asap.ASAPException;

import java.io.IOException;

public interface SharkCurrencyListenerNEW {
    void sharkCurrencyMessageReceived(CharSequence uri)
            throws ASAPException, IOException, SharkPromiseException;

}
