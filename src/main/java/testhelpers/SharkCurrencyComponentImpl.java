package testhelpers;

import Group.SharkGroupDocument;
import currency.api.SharkCurrencyComponent;
import currency.classes.Currency;
import currency.classes.GroupSignings;
import exepections.ASAPCurrencyException;
import listener.ASAPGroupInviteListener;
import listener.ASAPPromiseListener;
import net.sharksystem.*;
import net.sharksystem.asap.*;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.asap.crypto.ASAPKeyStore;
import net.sharksystem.asap.crypto.InMemoASAPKeyStore;
import net.sharksystem.asap.persons.SharkPKIFacadeImpl;
import net.sharksystem.asap.pki.ASAPCertificateStorage;
import net.sharksystem.asap.pki.ASAPStorageBasedCertificates;
import net.sharksystem.asap.pki.SharkPKIFacade;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.utils.SerializationHelper;
import net.sharksystem.utils.testsupport.TestConstants;

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

    private ASAPPeer asapPeer = null;
    private final SharkPKIComponent sharkPKIComponent;

    public SharkCurrencyComponentImpl(SharkPKIComponent sharkPKIComponent) throws SharkException {
        if(sharkPKIComponent == null) throw new SharkException("shark pki must not be null");
        this.sharkPKIComponent = sharkPKIComponent;
    }

    @Override
    public void establishGroup(Currency currency, ArrayList whitelistMember, boolean encrypted, boolean balanceVisible) throws ASAPCurrencyException {
        SharkGroupDocument sharkGroupDocument = new SharkGroupDocument(this.asapPeer.getPeerID(), currency, whitelistMember , encrypted, balanceVisible, GroupSignings.SIGNED_BY_NONE);
        try{
            // 1. Get Name of the Currency URI
            String currencyNameURI = currency.getCurrencyName();
            if (currencyNameURI == null || currencyNameURI.isEmpty()) {
                throw new ASAPCurrencyException("Currency URI cannot be empty");
            }

            // 2. sign document and add yourself to the group
            byte[] signature = ASAPCryptoAlgorithms
                    .sign(sharkGroupDocument.getGroupId(), asapKeyStore);
            if(signature == null || signature.length == 0) {
                System.err.println("CRITICAL: Signature could not be created! Check KeyStore for ID: "
                        + this.asapPeer.getPeerID());
            } else {
                System.out.println("SUCCESS: Created signature with length: " + signature.length);
            }
            boolean successAddMember = sharkGroupDocument
                    .addMember(this.asapPeer.getPeerID(),signature);
            if(!successAddMember) {
                throw new ASAPCurrencyException("Error in adding member to group");
            }

            // 3. Get ASAP Storage for our Currency Format
            ASAPStorage storage = this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);

            // 4. Create a new Channel for the specific Group
            storage.createChannel(currencyNameURI);
            System.out.println("DEBUG: all channels: "+storage.getChannelURIs());

            // 5. Serialize the document
            byte[] serializedDocument = sharkGroupDocument.toSaveByte();

            // 6. Save document in Storage
            storage.add(currencyNameURI, serializedDocument);

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

    private ASAPKeyStore asapKeyStore;

    //Sets-Up the PKI for our peer
    @Override
    public void onStart(ASAPPeer asapPeer) throws SharkException {
        System.out.println("DEBUG: Component started for peer: " + asapPeer.getPeerID());
        this.asapPeer = asapPeer;
        this.asapKeyStore = this.sharkPKIComponent.getASAPKeyStore();
        System.out.println("DEBUG (Impl): Keystore Hash: " + System.identityHashCode(this.asapKeyStore));
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
            byte[] unserializedDocument = messages.getMessage(0, false);
            return SharkGroupDocument.fromByte(unserializedDocument);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ASAPException e) {
            throw new ASAPException(e);
        }
     }

}
