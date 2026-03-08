package listener;

import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.pki.SharkPKIComponent;

/**
 * This Interface handles individual message types within the SharkCurrency Application.
 * For example.
 * You want to handle a group invite differently than being asked to sign a promise
 */
public interface SharkCurrencyMessageHandler {

    /**
     * logic for handling different kinds of messages will be implemented here
     *
     * @param uri uri of the message which tells us the type of message
     * @param storage
     * @param pki
     * @param sender
     */
    void handle(CharSequence uri, ASAPStorage storage, SharkPKIComponent pki, CharSequence sender);
}
