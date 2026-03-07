package testhelpers;

import currency.api.SharkCurrencyComponent;
import currency.classes.SharkPromise;
import currency.classes.SharkPromiseSerializer;
import exepections.SharkCurrencyException;
import listener.*;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.pki.SharkPKIComponent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//TODO: wenn wir fertig sind mit den neuen listenern dürfen wir nicht vergessen die zu adden
public class SharkCurrencyListenerImpl implements SharkCurrencyListenerNEW {

    private final SharkCurrencyComponentImpl sharkCurrencyComponent;
    private SharkPromise sharkPromise;
    private final Map<String, SharkCurrencyMessageHandler> handlers = new HashMap<>();

    public SharkCurrencyListenerImpl(SharkCurrencyComponent sharkCurrencyComponent) {
        handlers.put(SharkPromise.SHARK_PROMISE_ASK_FOR_SIGNATURE_AS_CRED,
                new SharkPromiseAskSigCredHandler());
        handlers.put(SharkPromise.SHARK_PROMISE_ASK_FOR_SIGNATURE_AS_DEB,
                new SharkPromiseAskSigDebHandler());
        handlers.put(SharkCurrencyComponent.INVITE_CHANNEL_URI,
                new SharkGroupInviteHandler());
        handlers.put(SharkCurrencyComponent.NEW_MEMBER_URI,
                new SharkNewMemberHandler());

        this.sharkCurrencyComponent = (SharkCurrencyComponentImpl) sharkCurrencyComponent;
    }

    @Override
    public void sharkCurrencyMessageReceived(CharSequence uri) {
        try {
            SharkCurrencyMessageHandler handler = handlers.get(uri.toString());
            if(handler==null) {
                throw new SharkCurrencyException("Could not find uri: " + uri);
            }
            ASAPStorage storage = this.sharkCurrencyComponent.getASAPStorage();
            SharkPKIComponent pki = this.sharkCurrencyComponent.getSharkPKIComponent();
            handler.handle(uri,storage,pki);
        } catch (IOException | ASAPException e) {
            throw new RuntimeException(e);
        }
    }

}
