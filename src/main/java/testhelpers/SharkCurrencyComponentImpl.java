package testhelpers;

import Group.SharkGroupDocument;
import currency.api.SharkCurrencyComponent;
import currency.classes.Currency;
import currency.classes.GroupSignings;
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
import net.sharksystem.utils.SerializationHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.sharksystem.utils.SerializationHelper.characterSequence2bytes;

/**
 * An implementation to test our Currency-Component which we need for testing
 * WORK IN PROGRESS!!!!!
 */
public class SharkCurrencyComponentImpl
        extends AbstractSharkComponent
        implements SharkComponent, SharkCurrencyComponent, ASAPMessageReceivedListener, ASAPEnvironmentChangesListener {

    private SharkPeer owner;
    private ASAPPeer asapPeer = null;
    private CharSequence ownerName;
    private ASAPPromiseListener promiseListener;
    private ASAPGroupInviteListener groupInviteListener = null;
    private final SharkPKIComponent sharkPKIComponent;

    public SharkCurrencyComponentImpl(SharkPKIComponent sharkPKIComponent) throws SharkException {
        if(sharkPKIComponent == null) throw new SharkException("shark pki must not be null");
        this.sharkPKIComponent = sharkPKIComponent;
    }

    @Override
    public void establishGroup(Currency currency, ArrayList whitelistMember, boolean encrypted, boolean balanceVisible) throws ASAPCurrencyException {
        SharkGroupDocument sharkGroupDocument = new SharkGroupDocument(this.asapPeer.toString(), currency, whitelistMember , encrypted, balanceVisible, GroupSignings.SIGNED_BY_NONE);
        try{
            // 1. Get Name of the Currency URI
            String currencyNameURI = currency.getCurrencyName();
            if (currencyNameURI == null || currencyNameURI.isEmpty()) {
                throw new ASAPCurrencyException("Currency URI cannot be empty");
            }
            // 2. Get ASAP Storage for our Currency Format
            ASAPStorage storage = this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);

            // 3. Create a new Channel for the specific Group
            storage.createChannel(currencyNameURI);
            System.out.println("DEBUG: all channels: "+storage.getChannelURIs());

            // 4. Serialize the document
            byte[] serializedDocument = sharkGroupDocument.toSaveByte();

            // 5. Save document in Storage
            storage.add(currencyNameURI, serializedDocument);


            // TODO: Serialize GroupDocument for whitelisted Members and send
            /**
            if (!whitelistMember.isEmpty() && whitelistMember != null){
                
            }

            **/

        } catch (IOException | ASAPException e){
            throw new ASAPCurrencyException(e.getMessage());
        }
    }

    @Override
    public void establishGroup(Currency currency, boolean encrypted, boolean balanceVisible) throws ASAPCurrencyException {
        // pass the method to the other establishGroup methode with null for whitelisted
        this.establishGroup(currency, null, encrypted, balanceVisible);
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
    private InMemoASAPKeyStore asapKeyStore;

    //Sets-Up the PKI for our peer
    @Override
    public void onStart(ASAPPeer asapPeer) throws SharkException {
        this.asapPeer = asapPeer;
        this.asapKeyStore = new InMemoASAPKeyStore(asapPeer.getPeerID());
    }

    @Override
    public void onlinePeersChanged(Set<CharSequence> set) {

    }

    @Override
    public void asapMessagesReceived(ASAPMessages asapMessages, String s, List<ASAPHop> list) throws IOException {

    }


    public SharkGroupDocument getSharkGroupDocument(CharSequence currencyNameUri) throws ASAPException {

        try {
            ASAPStorage storage = this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);
            ASAPChannel channel = storage.getChannel(currencyNameUri);
            ASAPMessages messages = channel.getMessages();
            byte[] unserializedDocument = messages.getMessage(0, true);
            return SharkGroupDocument.fromByte(unserializedDocument);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ASAPException e) {
            throw new ASAPException(e);
        }
     }

}
