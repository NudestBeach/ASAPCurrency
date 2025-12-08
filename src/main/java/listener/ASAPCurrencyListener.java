package listener;

import Group.SharkGroupDocument;
import currency.classes.Promise;
import exepections.WrongDestinationException;

/**
 * Listener interface for receiving notifications about currency events.
 * Implement this to update the UI or trigger business logic when transactions occur.
 * TODO: Muss noch eloscht werden
 */
public interface ASAPCurrencyListener {






    /**
     * Updates your balance. It happens when you receive incoming Promises and send outgoing Promises.
     * (If the Promise is signed by both parties)
     * @param currencyUri
     * @param newBalance
     * TODO: einen einzuigen Notifyer einführen
     */
    void onBalanceChangedNotify(String currencyUri, int newBalance);

}