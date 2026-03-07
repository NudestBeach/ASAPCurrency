package listener;

import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.pki.SharkPKIComponent;

/**
 * This Interface handles individual message types within the SharkCurrency Application.
 * For example.
 * You want to handle a group invite differently than being asked to sign a promise
 */
public interface SharkCurrencyMessageHandler {

    //logic for the individual message handling will be implemented here
    void handle(CharSequence uri, ASAPStorage storage, SharkPKIComponent pki);
}
