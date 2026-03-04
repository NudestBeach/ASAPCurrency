package listener;

import exepections.SharkPromiseException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.listenermanager.GenericNotifier;
import net.sharksystem.asap.listenermanager.GenericListenerImplementation;

import java.io.IOException;

public class SharkCurrencyListenerManagerNEW
        extends GenericListenerImplementation<SharkCurrencyListenerNEW> {

    public void addSharkBondReceivedListener(SharkCurrencyListenerNEW listener) {
        this.addListener(listener);
    }

    public void removeSharkBondReceivedListener(SharkCurrencyListenerNEW listener) {
        this.removeListener(listener);
    }

    protected void notifySharkBondReceivedListener(
            CharSequence uri) {

        SharkCurrencyNotifier sharkCurrencyNotifier =
                new SharkCurrencyNotifier(uri);

        this.notifyAll(sharkCurrencyNotifier, false);
    }

    private class SharkCurrencyNotifier implements GenericNotifier<SharkCurrencyListenerNEW> {
        private final CharSequence uri;

        public SharkCurrencyNotifier(CharSequence uri) {
            this.uri = uri;
        }

        @Override
        public void doNotify(SharkCurrencyListenerNEW sharkMessagesReceivedListener) {
            try {
                sharkMessagesReceivedListener.sharkCurrencyMessageReceived(this.uri);
            } catch (ASAPException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
