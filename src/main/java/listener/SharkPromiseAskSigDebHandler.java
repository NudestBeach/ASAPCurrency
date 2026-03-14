package listener;

import currency.classes.SharkPromise;
import currency.classes.SharkPromiseSerializer;
import currency.storage.SharkCurrencyStorage;
import exepections.SharkCurrencyException;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.pki.SharkPKIComponent;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class SharkPromiseAskSigDebHandler implements SharkCurrencyMessageHandler {

    private SharkCurrencyStorage currencyStorage;

    public SharkPromiseAskSigDebHandler(SharkCurrencyStorage currencyStorage) {
        this.currencyStorage=currencyStorage;
    }

    @Override
    public void handle(CharSequence uri, ASAPStorage storage, SharkPKIComponent pki, CharSequence sender) {
        try {
            ASAPMessages messages = storage.getChannel(uri).getMessages(false);
            System.out.println("DEBUG: received a message being asked to sign as debitor from: "
                    + sender + " message size is: " + messages.size());
            for (int i = 0; i < messages.size(); i++) {
                byte[] messageData = messages.getMessage(i, true);
                SharkPromise promise = SharkPromiseSerializer
                        .deserializePromise(messageData, pki.getASAPKeyStore());
                this.currencyStorage.addSharkPendingPromiseToStorage(promise);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
