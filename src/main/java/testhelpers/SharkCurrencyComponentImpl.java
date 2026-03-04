package testhelpers;

import currency.classes.SharkInMemoPromise;
import currency.classes.SharkPromise;
import currency.classes.SharkPromiseManagement;
import exepections.SharkPromiseException;
import group.SharkGroupDocument;
import currency.api.SharkCurrencyComponent;
import currency.classes.SharkCurrency;
import group.GroupSignings;
import exepections.SharkCurrencyException;
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
import java.util.*;

/**
 * An implementation to test our Currency-Component which we need for testing
 * WORK IN PROGRESS!!!!!
 */
public class SharkCurrencyComponentImpl
        extends SharkCurrencyListenerManager
        implements SharkCurrencyComponent, ASAPMessageReceivedListener {

    private final SharkPKIComponent sharkPKIComponent;
    private ASAPPeer asapPeer;

    public SharkCurrencyComponentImpl(SharkPKIComponent pki) throws SharkException {
        this.sharkPKIComponent = pki;
    }

    @Override
    public void establishGroup(SharkCurrency currency, ArrayList whitelistMember, boolean encrypted, boolean balanceVisible) throws SharkCurrencyException {
        this.checkComponentRunning();
        SharkGroupDocument sharkGroupDocument = new SharkGroupDocument(this.asapPeer.getPeerID(), currency, whitelistMember , encrypted, balanceVisible, GroupSignings.SIGNED_BY_NONE);
        try{
            // 1. Get Name of the Currency URI
            String currencyNameURI = currency.getCurrencyName();
            CharSequence groupURI = SharkGroupDocument.DOCUMENT_FORMAT + currencyNameURI;
            ASAPKeyStore ks = this.sharkPKIComponent.getASAPKeyStore();
            if (currencyNameURI == null || currencyNameURI.isEmpty()) {
                throw new SharkCurrencyException("Currency URI cannot be empty");
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
            System.out.println("DEBUG: added: " + this.asapPeer.getPeerID());
            if(!successAddMember) {
                throw new SharkCurrencyException("Error in adding member to group");
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
            throw new SharkCurrencyException(e.getMessage());
        }
    }

    @Override
    public void establishGroup(SharkCurrency currency, boolean encrypted, boolean balanceVisible) throws SharkCurrencyException {
        // pass the method to the other establishGroup methode with null for whitelisted
        this.establishGroup(currency, null, encrypted, balanceVisible);
    }

    @Override
    public void establishGroup(ArrayList<CharSequence> inviteMembers, SharkCurrency currency, ArrayList<CharSequence> whitelisted, boolean encrypted, boolean balanceVisible) throws SharkCurrencyException {

        // 0. Check method call validity
        this.checkComponentRunning();
        if (whitelisted != null && inviteMembers != null) {
            boolean allWhitelisted = inviteMembers.stream()
                    .allMatch(invited -> whitelisted.stream()
                            .anyMatch(white -> white.toString().equals(invited.toString())));

            if (!allWhitelisted) {
                throw new SharkCurrencyException("Can not invite peers that are not on the whitelist.");
            }
        }

    }

    @Override
    public void establishGroup(ArrayList<CharSequence> inviteMembers, SharkCurrency currency, boolean encrypted, boolean balanceVisible) throws SharkCurrencyException {

    }

    @Override
    public void sendPromise(CharSequence currencyName, CharSequence sender, Set<CharSequence> receiver, boolean sign, boolean encrypt, CharSequence uri) throws SharkCurrencyException {

    }

    @Override
    public void createPromise(int amount,
                              SharkCurrency referenceValue,
                              byte[] groupId,
                              CharSequence creditorId,
                              CharSequence debtorId,
                              boolean asCreditor) throws ASAPSecurityException, SharkPromiseException, IOException, SharkCurrencyException {
        SharkPromise promise =
                new SharkInMemoPromise(amount, referenceValue, groupId, creditorId, debtorId);
        Set<CharSequence> receiver = new HashSet<>();
        ASAPKeyStore keystore = this.sharkPKIComponent.getASAPKeyStore();
        CharSequence currencyName = promise.getReferenceValue().getCurrencyName();
        if(asCreditor) {
            SharkPromiseManagement
                    .signAsCreditor(keystore, promise);
            receiver.add(promise.getDebtorID());
            this.sendPromise(currencyName,
                    promise.getCreditorID(),
                    receiver,
                    true,
                    true,
                    SharkPromise.SHARK_PROMISE_ASK_FOR_SIGNATURE_AS_DEB);
        } else {
            SharkPromiseManagement
                    .signAsDebtor(keystore, promise);
            receiver.add(promise.getCreditorID());
            this.sendPromise(currencyName,
                    promise.getDebtorID(),
                    receiver,
                    true,
                    true,
                    SharkPromise.SHARK_PROMISE_ASK_FOR_SIGNATURE_AS_CRED);
        }
    }

    @Override
    public int getBalance(CharSequence currencyName) throws SharkCurrencyException {
        return 0;
    }

    @Override
    public void acceptInviteAndSign(CharSequence currencyName) throws ASAPException, IOException {
        SharkGroupDocument sharkGroupDocument = this.getSharkGroupDocument(currencyName);
        if(sharkGroupDocument==null) {
            throw new SharkCurrencyException("Can not accept and sign document because it is null");
        } else {
            ASAPKeyStore ks = this.sharkPKIComponent.getASAPKeyStore();
            byte[] signature = ASAPCryptoAlgorithms
                    .sign(sharkGroupDocument.getGroupId(), ks);
            sharkGroupDocument.addMember(this.asapPeer.getPeerID(), signature);
            byte[] newDocSerialized = sharkGroupDocument.sharkDocumentToByte();
            this.asapPeer.getASAPStorage(CURRENCY_FORMAT).add(SharkGroupDocument.DOCUMENT_FORMAT+currencyName, newDocSerialized);
            this.acceptInvite(sharkGroupDocument);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            String peerID = this.asapPeer.getPeerID().toString();
            try {
                dos.writeUTF(peerID);
                dos.writeUTF(currencyName.toString());
                dos.writeInt(signature.length);
                dos.write(signature);
                dos.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            byte[] signatureAndIDAsContent = baos.toByteArray();
            this.asapPeer.sendASAPMessage(CURRENCY_FORMAT, NEW_MEMBER_URI, signatureAndIDAsContent);
        }
    }

    //Sets-Up the PKI for our peer
    @Override
    public void onStart(ASAPPeer asapPeer) throws SharkException {
        this.asapPeer = asapPeer;
        try {
            // Initialize storage for peer to listen to the ASAPCurrency application
            this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);
            ASAPStorage storage = this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);
            // Create a new channel for different notis on start of a peer:
            storage.createChannel(INVITE_CHANNEL_URI);
            storage.createChannel(NEW_MEMBER_URI);
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
            SharkGroupDocument base = null;
            for (int i = 0; i < messages.size(); i++) {
                try {
                    SharkGroupDocument doc = parseSharkGroupDocument(
                            messages.getMessage(i, true));
                    if (doc == null) continue;

                    if (base == null) {
                        base = doc;
                    } else {
                        for (Map.Entry<String, byte[]> entry :
                                doc.getCurrentMembers().entrySet()) {
                            base.addMember(entry.getKey(), entry.getValue());
                        }
                    }
                } catch (Exception e) {
                }
            }
            System.out.println("DEBUG: " + this.asapPeer.getPeerID() + " messages size: " + messages.size() + " Group size: " + base.getCurrentMembers().size());
            return base;
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

    private void checkComponentRunning() throws SharkCurrencyException {
        if(this.asapPeer == null || this.sharkPKIComponent == null)
            throw new SharkCurrencyException("peer not started and/or pki not initialized");
    }

    // Bearbeitet: sendASAPMessage (persistent) statt sendTransientASAPMessage
    @Override
    public void invitePeerToGroup(CharSequence currencyNameUri, String optionalMessage, CharSequence peerId)
            throws SharkCurrencyException {

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
            throw new SharkCurrencyException("Fehler bei Einladung: " + e.getLocalizedMessage());
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
                return; // add these returns, they are important
            }
            if(uri.toString().equals(NEW_MEMBER_URI)) {
                System.out.println("DEBUG: New member notification received!");
                CharSequence currencyName = receivedNewMemberNoti(asapMessages,sender, this);
                return; // add these returns, they are important
            }

            ASAPStorage storage = this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);
            for (int i = 0; i < asapMessages.size(); i++) {
                byte[] msgContent = asapMessages.getMessage(i, false);

                SharkGroupDocument doc = parseSharkGroupDocument(msgContent);

                if (doc != null && doc.getAssignedCurrency() != null) {

                    // 1. signiere das Group Document (Einladung annehmen)
                    byte[] mySignature = net.sharksystem.asap.crypto.ASAPCryptoAlgorithms.sign(
                            doc.getGroupId(), this.sharkPKIComponent.getASAPKeyStore());

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

    public String receivedNewMemberNoti(ASAPMessages message, String sender, SharkCurrencyComponent scc) {
        try {
            System.out.println("DEBUG: I "+this.asapPeer.getPeerID()+" received a new message from: " + sender + " message size is: " + message.size());
            int lastindex = message.size()-1;
            byte[] messageData = message.getMessage(lastindex, true);

            //--------Read all data from the message ------------------------
            ByteArrayInputStream bais = new ByteArrayInputStream(messageData);
            DataInputStream dis = new DataInputStream(bais);
            String peerID = dis.readUTF();
            String currencyName = dis.readUTF();
            int sigLength = dis.readInt();
            byte[] signature = new byte[sigLength];
            dis.readFully(signature);
            //---------------------------------------------------------------
            CharSequence groupURI = SharkGroupDocument.DOCUMENT_FORMAT + currencyName;
            ASAPStorage storage = this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);
            SharkGroupDocument doc = this.getSharkGroupDocument(currencyName);
            System.out.println("DEBUG: VOR addMember - doc hat Members: " + doc.getCurrentMembers().keySet());
            doc.addMember(peerID, signature);
            System.out.println("DEBUG: NACH addMember - doc hat Members: " + doc.getCurrentMembers().keySet());
            storage.add(groupURI, doc.sharkDocumentToByte());
            System.out.println("Added"  + peerID + " to " + groupURI + " iam " + this.asapPeer.getPeerID() + " so member size is now: " + doc.getCurrentMembers());
            return currencyName;
        } catch (ASAPException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ASAPStorage getASAPStorage() throws IOException, ASAPException {
        return this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);
    }

    public SharkPKIComponent getSharkPKIComponent() {
        return this.sharkPKIComponent;
    }
}
