package listener;

import Group.SharkGroupDocument;
import currency.classes.Promise;

/**
 * Listener interface for receiving notifications about currency events.
 * Implement this to update the UI or trigger business logic when transactions occur.
 */
public interface ASAPCurrencyListener {

    /**
     *
     * @param currencyUri
     * @param promise
     */
    void onPromiseReceived(String currencyUri, Promise promise, byte[] receiverId);

    /**
     * Updates your balance. It happens when you receive incoming Promises and send outgoing Promises.
     * (If the Promise is signed by both parties)
     * @param currencyUri
     * @param newBalance
     */
    void onBalanceChanged(String currencyUri, int newBalance);

    void onGroupInviteReceived(String currencyUri, SharkGroupDocument groupDocument, byte[] receiverId) throws WrongDestinationExeption;
}