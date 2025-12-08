package listener;

import Group.SharkGroupDocument;
import currency.classes.Promise;

/**
 * Listener interface for receiving notifications about currency events.
 * Implement this to update the UI or trigger business logic when transactions occur.
 */
public interface ASAPCurrencyListener {


    /**
     * If you get via Gossip a message that you can join a Group this method will be called.
     * @param currencyUri The group you want to join
     * @param groupDocument The group document with Metadata
     * @throws WrongDestinationException If the message does not belong to you.
     * TODO: SharkInviteMessage mit dem Nötigsten
     */
    void onGroupInviteReceived(String currencyID, SharkGroupDocument groupDocument) throws WrongDestinationException;


    /**
     * Called, when a new Promise comes in.
     * @param currencyUri
     * @param promise
     */
    void onPromiseReceived(String currencyUri, Promise promise);

    /**
     * Updates your balance. It happens when you receive incoming Promises and send outgoing Promises.
     * (If the Promise is signed by both parties)
     * @param currencyUri
     * @param newBalance
     * TODO: einen einzuigen Notifyer einführen
     */
    void onBalanceChangedNotify(String currencyUri, int newBalance);

}