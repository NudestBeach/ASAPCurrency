package testhelpers;

import currency.api.SharkCurrencyComponent;
import currency.classes.SharkPromise;
import currency.classes.SharkPromiseSerializer;
import exepections.SharkCurrencyException;
import listener.SharkCurrencyListenerNEW;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.pki.SharkPKIComponent;

import java.io.IOException;
import java.util.Set;

public class SharkCurrencyListenerImpl implements SharkCurrencyListenerNEW {

    private final SharkCurrencyComponentImpl sharkCurrencyComponent;
    private SharkPromise sharkPromise;

    public SharkCurrencyListenerImpl(SharkCurrencyComponent sharkCurrencyComponent) {
        this.sharkCurrencyComponent = (SharkCurrencyComponentImpl) sharkCurrencyComponent;
    }

    @Override
    public void sharkCurrencyMessageReceived(CharSequence uri) {
        CharSequence sender;
        Set<CharSequence> receiver;
        try {
            ASAPStorage asapStorage = this.sharkCurrencyComponent.getASAPStorage();
            byte[] asapMessage = asapStorage
                    .getChannel(uri)
                    .getMessages(false)
                    .getMessage(0, true);
            SharkPKIComponent sharkPKIComponent =
                    this.sharkCurrencyComponent.getSharkPKIComponent();
            this.sharkPromise = SharkPromiseSerializer
                    .deserialzePromise(asapMessage, sharkPKIComponent.getASAPKeyStore());
            switch(uri.toString()) {
                //TODO die Cases noch ausfuhren
                default:
                    throw new SharkCurrencyException("Uri was not found");
                case SharkCurrencyComponent.INVITE_CHANNEL_URI:
                    break;
                case SharkCurrencyComponent.NEW_MEMBER_URI:
                    break;
                case SharkPromise.SHARK_PROMISE_ASK_FOR_SIGNATURE_AS_CRED:
                    break;
                case SharkPromise.SHARK_PROMISE_ASK_FOR_SIGNATURE_AS_DEB:
                    break;
            }
        } catch (ASAPException | IOException e) {
            System.out.println(e.getMessage());
        }

    }

}
