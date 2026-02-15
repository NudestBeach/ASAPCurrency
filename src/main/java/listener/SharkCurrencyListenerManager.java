package listener;


import net.sharksystem.asap.listenermanager.ASAPMessageReceivedListenerManager;
import net.sharksystem.asap.listenermanager.GenericListenerImplementation;

import java.util.HashMap;

public class SharkCurrencyListenerManager extends GenericListenerImplementation<SharkCurrencyListener> {

    private HashMap<CharSequence, SharkCurrencyListener> listenerMap = new HashMap();

    public void addSharkCurrencyListener(SharkCurrencyListener listener) {
        this.addListener(listener);
    }

    public void removeSharkCurrencyListener(SharkCurrencyListener listener) {
        this.removeListener(listener);
    }

    protected void notifySharkCurrencyNotiReceived(CharSequence uri) {

    }

    private class SharkCurrencyNotiReceivedNotifier implements GenericNotifier<SharkCurrencyListener> {
        private final CharSequence uri;

        public SharkCurrencyNotiReceivedNotifier(CharSequence uri) {
            this.uri = uri;
        }

        @Override
        public void doNotify(SharkCurrencyListener sharkMessagesReceivedListener) {
            sharkMessagesReceivedListener.handleSharkCurrencyNotification(this.uri);
        }
    }

}
