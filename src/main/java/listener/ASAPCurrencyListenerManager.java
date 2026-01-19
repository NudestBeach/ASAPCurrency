package listener;


import net.sharksystem.asap.listenermanager.ASAPMessageReceivedListenerManager;
import net.sharksystem.asap.listenermanager.GenericListenerImplementation;

import java.util.HashMap;

public class ASAPCurrencyListenerManager extends GenericListenerImplementation<ASAPCurrencyListener> {

    private HashMap<CharSequence, GenericListenerImplementation<ASAPCurrencyListener> listenerMap =
            new HashMap();

    public void addSharkCurrencyListener(ASAPCurrencyListener listener) {
        this.addListener(listener);
    }

    public void removeSharkCurrencyListener(ASAPCurrencyListener listener) {
        this.removeListener(listener);
    }

    protected void sharkCurrencNotifyReceived(CharSequence format) {

    }

}
