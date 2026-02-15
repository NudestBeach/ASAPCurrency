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

    /**
     * This method manages all incoming notifications regariding the currency application scope.
     * It will then lead the notifivation to the right listener, which is responsible for the task.
     * @param uri Channel in which the notification was send.
     */
    void handleSharkCurrencyNotification(CharSequence uri);
}