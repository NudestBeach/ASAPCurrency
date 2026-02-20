package testhelpers;

import Group.SharkGroupDocument;
import currency.api.SharkCurrencyComponent;
import currency.classes.Currency;
import Group.GroupSignings;
import exepections.ASAPCurrencyException;
import listener.SharkCurrencyListenerManager;
import net.sharksystem.*;
import net.sharksystem.asap.*;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.asap.crypto.ASAPKeyStore;
import net.sharksystem.pki.SharkPKIComponent;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation to test our Currency-Component which we need for testing
 * WORK IN PROGRESS!!!!!
 */
public class SharkCurrencyComponentImpl
        extends SharkCurrencyListenerManager
        implements SharkCurrencyComponent, ASAPMessageReceivedListener {

    public final String INVITE_CHANNEL_URI = "//group-document//invite";
    private final SharkPKIComponent sharkPKIComponent;
    private ASAPPeer asapPeer;

    public SharkCurrencyComponentImpl(SharkPKIComponent pki) throws SharkException {
        this.sharkPKIComponent = pki;
    }

    @Override
    public void establishGroup(Currency currency, ArrayList whitelistMember, boolean encrypted, boolean balanceVisible) throws ASAPCurrencyException {
        this.checkComponentRunning();
        SharkGroupDocument sharkGroupDocument = new SharkGroupDocument(this.asapPeer.getPeerID(), currency, whitelistMember , encrypted, balanceVisible, GroupSignings.SIGNED_BY_NONE);
        try{
            // 1. Get Name of the Currency URI
            String currencyNameURI = currency.getCurrencyName();
            CharSequence groupURI = SharkGroupDocument.DOCUMENT_FORMAT + currencyNameURI;
            ASAPKeyStore ks = this.sharkPKIComponent.getASAPKeyStore();
            if (currencyNameURI == null || currencyNameURI.isEmpty()) {
                throw new ASAPCurrencyException("Currency URI cannot be empty");
            }

            // 2. sign document and add yourself to the group
            byte[] signature = ASAPCryptoAlgorithms
                    .sign(sharkGroupDocument.getGroupId(), ks);
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
            storage.createChannel(groupURI);

            // 5. Serialize the document
            byte[] serializedDocument = sharkGroupDocument.sharkDocumentToByte();

            // 6. Save document in Storage
            storage.add(groupURI, serializedDocument);

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
    public void establishGroup(ArrayList<CharSequence> inviteMembers, Currency currency, ArrayList<CharSequence> whitelisted, boolean encrypted, boolean balanceVisible) throws ASAPCurrencyException {

        // 0. Check method call validity
        this.checkComponentRunning();
        if (whitelisted != null && inviteMembers != null) {
            boolean allWhitelisted = inviteMembers.stream()
                    .allMatch(invited -> whitelisted.stream()
                            .anyMatch(white -> white.toString().equals(invited.toString())));

            if (!allWhitelisted) {
                throw new ASAPCurrencyException("Can not invite peers that are not on the whitelist.");
            }
        }

    }

    @Override
    public void establishGroup(ArrayList<CharSequence> inviteMembers, Currency currency, boolean encrypted, boolean balanceVisible) throws ASAPCurrencyException {

    }

    @Override
    public void sendPromise(CharSequence currencyName, CharSequence recipientId, int amount, CharSequence note) throws ASAPCurrencyException {

    }

    @Override
    public int getBalance(CharSequence currencyName) throws ASAPCurrencyException {
        return 0;
    }

    //Sets-Up the PKI for our peer
    @Override
    public void onStart(ASAPPeer asapPeer) throws SharkException {
        this.asapPeer = asapPeer;
        try {
            // Initialisiere Storage, damit der Peer auf dieses Format hört
            this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);
            ASAPStorage storage = this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);

            // Create a new Channel for the specific Group invites
            storage.createChannel(INVITE_CHANNEL_URI);
        } catch (IOException e) {
            throw new SharkException("Could not initialize ASAP storage for currency", e);
        }

        this.asapPeer.addASAPMessageReceivedListener(SharkCurrencyComponent.CURRENCY_FORMAT,
                this);
    }

    // Hilfsmethode zum robusten Parsen
    private SharkGroupDocument parseSharkGroupDocument(byte[] data) {
        if (data == null || data.length == 0) return null;

        try {
            // Versuch 1: Direktes Parsen (für nackte Dokumente im Storage)
            return SharkGroupDocument.fromByte(data);
        } catch (Exception e) {
            // Versuch 2: Parsen eines gewrappten Pakets (aus invitePeerToGroup)
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 DataInputStream dis = new DataInputStream(bais)) {

                dis.readUTF(); // Überspringe die 'optionalMessage'
                int docLength = dis.readInt(); // Lies die Länge des Dokuments

                if (docLength > 0 && bais.available() >= docLength) {
                    byte[] docBytes = new byte[docLength];
                    dis.readFully(docBytes);
                    return SharkGroupDocument.fromByte(docBytes);
                }
            } catch (Exception innerEx) {
                // Falls beides fehlschlägt
            }
        }
        return null;
    }

    public SharkGroupDocument getSharkGroupDocument(CharSequence currencyNameUri) throws ASAPException {

        CharSequence groupUri = SharkGroupDocument.DOCUMENT_FORMAT + currencyNameUri.toString();

        try {
            ASAPStorage storage = this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);
            ASAPChannel channel = storage.getChannel(groupUri);
            ASAPMessages messages = channel.getMessages();
            if (messages.size() == 0) {
                System.err.println("DEBUG: No messages found in channel " + groupUri);
                return null;
            }

            SharkGroupDocument doc = parseSharkGroupDocument(messages.getMessage(0, false));
            if (doc == null) {
                throw new ASAPException("Konnte SharkGroupDocument nicht parsen.");
            }
            return doc;
        } catch (IOException e){
            throw new RuntimeException(e);
        } catch (ASAPException e){
            throw new ASAPException(e);
        }
    }

     public byte[] getSharkGroupDocumentSerialized(CharSequence currencyNameUri) {
         CharSequence groupUri = SharkGroupDocument.DOCUMENT_FORMAT + currencyNameUri;

         try {
             ASAPStorage storage = this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);
             ASAPChannel channel = storage.getChannel(groupUri);
             ASAPMessages messages = channel.getMessages();
             if(messages.size() == 0) {
                 System.err.println("DEBUG: No messages found in channel " + groupUri);
                 return null;
             }
             return messages.getMessage(0, false);

         } catch (IOException | ASAPException e) {
             throw new RuntimeException(e);
         }
     }

    private void checkComponentRunning() throws ASAPCurrencyException {
        if(this.asapPeer == null || this.sharkPKIComponent == null)
            throw new ASAPCurrencyException("peer not started and/or pki not initialized");
    }

    // Bearbeitet: sendASAPMessage (persistent) statt sendTransientASAPMessage
    @Override
    public void invitePeerToGroup(CharSequence currencyNameUri, String optionalMessage, CharSequence peerId)
            throws ASAPCurrencyException {

        this.checkComponentRunning();

        try {
             byte[] docBytes = this.getSharkGroupDocumentSerialized(currencyNameUri);

             ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream daos = new DataOutputStream(baos);

             daos.writeUTF(optionalMessage != null ? optionalMessage : "");
             daos.writeInt(docBytes.length);
            daos.write(docBytes);

            byte[] fullContentOfInvite = baos.toByteArray();
            CharSequence inviteURI = INVITE_CHANNEL_URI;
            this.asapPeer.sendASAPMessage(CURRENCY_FORMAT, inviteURI, fullContentOfInvite);

        } catch(ASAPException | IOException e) {
            throw new ASAPCurrencyException("Fehler bei Einladung: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void asapMessagesReceived(ASAPMessages asapMessages, String sender, List<ASAPHop> list) throws IOException {
        try {
            //Checking URI
            CharSequence uri = asapMessages.getURI();
            if(uri.toString().equals(INVITE_CHANNEL_URI)) {
                System.out.println("DEBUG: Invite received!");
                receivedInvite(asapMessages, sender);
                return;
            }

            ASAPStorage storage = this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);
            for (int i = 0; i < asapMessages.size(); i++) {
                byte[] msgContent = asapMessages.getMessage(i, false);

                SharkGroupDocument doc = parseSharkGroupDocument(msgContent);

                if (doc != null && doc.getAssignedCurrency() != null) {

                    // 1. signiere das Group Document (Einladung annehmen)
                    byte[] mySignature = net.sharksystem.asap.crypto.ASAPCryptoAlgorithms.sign(
                            doc.getGroupId(), this.sharkPKIComponent.getASAPKeyStore());

                    doc.addMember(this.asapPeer.getPeerID(), mySignature);

                    // 2. speicher das Group Dokument ab
                    String cName = doc.getAssignedCurrency().getCurrencyName();
                    CharSequence targetGroupUri = SharkGroupDocument.DOCUMENT_FORMAT + cName;

                    // Kanal erstellen, falls er noch nicht existiert
                    if (!storage.channelExists(targetGroupUri)) {
                        storage.createChannel(targetGroupUri);
                    }

                    storage.add(targetGroupUri, doc.sharkDocumentToByte());

                    System.out.println("DEBUG: Auto-accepted and cleaned Invite for " + cName);
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Verarbeiten der empfangenen Nachricht: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
