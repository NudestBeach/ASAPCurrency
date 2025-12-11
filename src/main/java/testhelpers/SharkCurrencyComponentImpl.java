package testhelpers;

import currency.api.SharkCurrencyComponent;
import currency.classes.Currency;
import exepections.ASAPCurrencyException;
import listener.ASAPGroupInviteListener;
import listener.ASAPPromiseListener;
import net.sharksystem.AbstractSharkComponent;
import net.sharksystem.SharkComponent;
import net.sharksystem.SharkException;
import net.sharksystem.SharkPeer;
import net.sharksystem.asap.*;
import net.sharksystem.asap.crypto.InMemoASAPKeyStore;
import net.sharksystem.asap.persons.SharkPKIFacadeImpl;
import net.sharksystem.asap.pki.ASAPCertificateStorage;
import net.sharksystem.asap.pki.ASAPStorageBasedCertificates;
import net.sharksystem.asap.pki.SharkPKIFacade;
import net.sharksystem.pki.SharkPKIComponent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * An implementation to test our Currency-Component which we need for testing
 * WORK IN PROGRESS!!!!!
 */
public class SharkCurrencyComponentImpl
        extends AbstractSharkComponent
        implements SharkComponent, SharkCurrencyComponent, ASAPMessageReceivedListener, ASAPEnvironmentChangesListener {

    private SharkPeer owner;
    private CharSequence ownerName;
    private ASAPPromiseListener promiseListener;
    private ASAPGroupInviteListener groupInviteListener = null;

    private final SharkPKIComponent sharkPKIComponent;

    public SharkCurrencyComponentImpl(SharkPKIComponent sharkPKIComponent) {
        this.sharkPKIComponent = sharkPKIComponent;
    }

    @Override
    public void establishGroup(Currency currency, ArrayList whitelisted, boolean encrypted, boolean balanceVisible) throws ASAPCurrencyException {

    }

    @Override
    public void establishGroup(Currency currency, boolean encrypted, boolean balanceVisible) throws ASAPCurrencyException {

    }

    @Override
    public void sendPromise(CharSequence currencyName, CharSequence recipientId, int amount, CharSequence note) throws ASAPCurrencyException {

    }

    @Override
    public int getBalance(CharSequence currencyName) throws ASAPCurrencyException {
        return 0;
    }

    /////////////////////////// PKI-Setup /////////////////////////////
    /// Our Peers need a KeyStore to sign GroupInvites or Promises ///
    public static final String SHARK_PKI_DATA_KEY = "sharkPKIData";
    private SharkPKIFacade sharkPKIFacade = null;
    private ASAPStorageBasedCertificates asapCertificateStorage;
    private ASAPPeer asapPeer = null;
    private InMemoASAPKeyStore asapKeyStore;

    //Sets-Up the PKI for our peer
    @Override
    public void onStart(ASAPPeer asapPeer) throws SharkException {
        //its a keystore bro
        this.asapKeyStore = new InMemoASAPKeyStore(asapPeer.getPeerID());
        try {
            this.asapKeyStore.setMementoTarget(this.owner.getSharkPeerExtraData());
            ASAPStorage asapStorage = asapPeer.getASAPStorage(ASAPCertificateStorage.PKI_APP_NAME);
            CharSequence peerName = this.ownerName != null ? this.ownerName : asapPeer.getPeerID();

            this.asapCertificateStorage =
                    new ASAPStorageBasedCertificates(asapStorage, asapPeer.getPeerID(), peerName);

            this.sharkPKIFacade =
                    new SharkPKIFacadeImpl(this.asapCertificateStorage, this.asapKeyStore);
            //Memento means it will be persistend the state after the system shuts down
            //its like saving a gamefile and return at the game after reboot
            this.sharkPKIFacade.setMementoTarget(this.owner.getSharkPeerExtraData());
            //This for notify you if a peer is in hood / nachbarschaft
            this.asapPeer.addASAPEnvironmentChangesListener(this);
            this.asapPeer.addASAPMessageReceivedListener(SharkPKIComponent.PKI_APP_NAME, this);


        } catch (IOException | ASAPException e) {
            throw new SharkException(e);
        }
    }

    @Override
    public void onlinePeersChanged(Set<CharSequence> set) {

    }

    @Override
    public void asapMessagesReceived(ASAPMessages asapMessages, String s, List<ASAPHop> list) throws IOException {

    }
}
