package implementations;

import currency.classes.SharkInMemoPromise;
import currency.classes.SharkPromise;
import currency.classes.SharkPromiseManagement;
import currency.storage.SharkCurrencyStorage;
import exepections.SharkPromiseException;
import group.SharkGroupDocument;
import currency.api.SharkCurrencyComponent;
import currency.classes.SharkCurrency;
import group.GroupSignings;
import exepections.SharkCurrencyException;
import listener.SharkCurrencyListener;
import listener.SharkCurrencyListenerManager;
import listener.SharkCurrencyListenerManagerNEW;
import listener.SharkCurrencyListenerNEW;
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
        extends SharkCurrencyListenerManagerNEW
        implements SharkCurrencyComponent, ASAPMessageReceivedListener {

    private final SharkPKIComponent sharkPKIComponent;
    private ASAPPeer asapPeer;
    private SharkCurrencyListenerNEW sharkCurrencyListenerNEW;
    private SharkCurrencyStorage sharkCurrencyStorage;

    public SharkCurrencyComponentImpl(SharkPKIComponent pki) throws SharkException {
        this.sharkPKIComponent = pki;
    }

    @Override
    public byte[] establishGroup(SharkCurrency currency, ArrayList<CharSequence> whitelistMember, boolean encrypted, boolean balanceVisible) throws SharkCurrencyException {
        this.checkComponentRunning();
        SharkGroupDocument sharkGroupDocument = new SharkGroupDocument(this.asapPeer.getPeerID(), currency, whitelistMember , encrypted, balanceVisible, GroupSignings.SIGNED_BY_NONE);
        try{
            // 1. Get Name of the Currency URI
            byte[] groupId = sharkGroupDocument.getGroupId();
            ASAPKeyStore ks = this.sharkPKIComponent.getASAPKeyStore();

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

            // 3. save the newly created document
            this.sharkCurrencyStorage.saveGroupDocument(groupId, sharkGroupDocument);
            return groupId;

        } catch (ASAPException e){
            throw new SharkCurrencyException(e.getMessage());
        }
    }

    @Override
    public byte[] establishGroup(SharkCurrency currency, boolean encrypted, boolean balanceVisible) throws SharkCurrencyException {
        // pass the method to the other establishGroup methode with null for whitelisted
        byte[] groupId = this.establishGroup(currency, null, encrypted, balanceVisible);
        return groupId;
    }

    @Override
    public byte[] establishGroup(ArrayList<CharSequence> inviteMembers, SharkCurrency currency, ArrayList<CharSequence> whitelisted, boolean encrypted, boolean balanceVisible) throws SharkCurrencyException {

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
        return null; //TODO...
    }

    @Override
    public byte[] establishGroup(ArrayList<CharSequence> inviteMembers, SharkCurrency currency, boolean encrypted, boolean balanceVisible) throws SharkCurrencyException {
        return null;
    }

    @Override
    public void sendPromise(CharSequence currencyName, CharSequence sender, Set<CharSequence> receiver, boolean sign, boolean encrypt, CharSequence uri) throws SharkCurrencyException {

    }

    @Override
    public CharSequence createPromise(int amount,
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
            return promise.getPromiseID();
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
            return promise.getPromiseID();
        }
    }

    @Override
    public int getBalance(CharSequence currencyName) throws SharkCurrencyException {
        return 0;
    }

    @Override
    public void acceptInviteAndSign(CharSequence currencyName) throws ASAPException, IOException {

        //get doc from pending invites
        SharkGroupDocument sharkGroupDocument
                = this.sharkCurrencyStorage.getPendingInvite(currencyName.toString());
        byte[] groupId = sharkGroupDocument.getGroupId();

        //sign doc and add yourself
        ASAPKeyStore ks = this.sharkPKIComponent.getASAPKeyStore();
        byte[] signature = ASAPCryptoAlgorithms
                .sign(sharkGroupDocument.getGroupId(), ks);
        sharkGroupDocument.addMember(this.asapPeer.getPeerID(), signature);

        //safe doc to your storage
        this.sharkCurrencyStorage.saveGroupDocument(groupId,sharkGroupDocument);

        //remove the pending invite since you accepted
        this.sharkCurrencyStorage.removePendingInvite(currencyName.toString());

        //package your member data for notifying the members
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        String peerID = this.asapPeer.getPeerID().toString();
        dos.writeUTF(peerID);
        dos.writeInt(groupId.length);
        dos.write(groupId);
        dos.writeInt(signature.length);
        dos.write(signature);
        dos.flush();

        byte[] signatureAndIDAsContent = baos.toByteArray();
        this.asapPeer.sendASAPMessage(CURRENCY_FORMAT, NEW_MEMBER_URI, signatureAndIDAsContent);
    }

    @Override
    public void declineInvite(CharSequence currencyName) {
        this.sharkCurrencyStorage.removePendingInvite(currencyName.toString());
    }

    //Sets-Up the PKI for our peer
    @Override
    public void onStart(ASAPPeer asapPeer) throws SharkException {
        this.asapPeer = asapPeer;
        try {
            // Initialize storage for peer to listen to the ASAPCurrency application
            this.asapPeer.getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);
            this.sharkCurrencyStorage = new SharkCurrencyStorageImpl();
            this.asapPeer.addASAPMessageReceivedListener(SharkCurrencyComponent.CURRENCY_FORMAT, this);
        } catch (IOException e) {
            throw new SharkException("Could not initialize ASAP storage for currency", e);
        }
    }

    // Hilfsmethode zum robusten Parsen
    public static SharkGroupDocument parseSharkGroupDocument(byte[] data) {
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
                System.err.println("DEBUG: A No messages found in channel " + groupUri);
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
                 System.err.println("DEBUG: B No messages found in channel " + groupUri);
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

    @Override
    public void invitePeerToGroup(byte[] groupId, String optionalMessage, CharSequence peerId)
            throws SharkCurrencyException {

        this.checkComponentRunning();
        SharkGroupDocument sharkGroupDocument = this.sharkCurrencyStorage.getGroupDocument(groupId);

        try {
            if(!sharkGroupDocument.getWhitelistMember().contains(peerId)) {
                throw new SharkCurrencyException("Peer with id: " + peerId + " can not be invited because this peer is not whitelisted.");
            }

             byte[] docBytes =  sharkGroupDocument.sharkDocumentToByte();

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
            CharSequence uri = asapMessages.getURI();
            this.notifySharkCurrencyListener(uri);
        } catch (NullPointerException e) {
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

    public CharSequence getPeerIdOfImpl() {
        return this.asapPeer.getPeerID();
    }

    @Override
    public SharkCurrencyStorage getSharkCurrencyStorage() {
        return this.sharkCurrencyStorage;
    }

    @Override
    public void subscribeSharkCurrencyListener(SharkCurrencyListenerNEW listener) {
        this.sharkCurrencyListenerNEW = listener;
        this.addSharkCurrencyListener(listener);
    }
}
