package listener;


import net.sharksystem.asap.listenermanager.ASAPMessageReceivedListenerManager;
import net.sharksystem.asap.listenermanager.GenericListenerImplementation;

import java.util.HashMap;

public class ASAPCurrencyListenerManager extends GenericListenerImplementation<ASAPCurrencyListener> {

    private HashMap<CharSequence, ASAPCurrencyListener> listenerMap = new HashMap();

    public void addSharkCurrencyListener(CharSequence uri, ASAPCurrencyListener listener) {
        if(!listenerMap.containsKey(uri)) {
            listenerMap.put(uri, listener);
        }
        this.addListener(listenerMap.get(uri));
    }

    public void notifySharkMessageReceivedListener(
            CharSequence uri) {

        SharkMessagesReceivedNotifier sharkMessagesReceivedNotifier =
                new SharkMessagesReceivedNotifier(uri);

        this.notifyAll(null, false);
    }

    private class SharkMessagesReceivedNotifier implements GenericNotifier<ASAPCurrencyListener> {
        private final CharSequence uri;

        public SharkMessagesReceivedNotifier(CharSequence uri) {
            this.uri = uri;
        }

        @Override
        public void doNotify(ASAPCurrencyListener sharkMessagesReceivedListener) {
            sharkMessagesReceivedListener.handleSharkCurrencyNotification(this.uri, null);
        }
    }

}
