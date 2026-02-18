package listener;

import Group.SharkGroupDocument;
import currency.classes.Promise;
import exepections.WrongDestinationException;

/**
 * Listener interface for receiving notifications about currency events.
 * Implement this to update the UI or trigger business logic when transactions occur.
 * You may handle group invites, receiving promises and more
 */
public interface SharkCurrencyListener {

    void handleSharkCurrencyNotification(CharSequence uri);

    /**
     * When an invite arrives this method is called
     * @param sharkGroupDocument the document of the group
     * @param sender PeerID of sender
     * @param message Optional message of invite, might be empty
     */
    void onInviteReceived(SharkGroupDocument sharkGroupDocument, String sender, String message);

    /**
     * Will be called when the user accepts the group invite
     */
    void onInviteAccepted(SharkGroupDocument sharkGroupDocument);

    /**
     * Will be called when the user declines the group invite
     */
    void onInviteDeclined(SharkGroupDocument sharkGroupDocument);
}