package Group;

import currency.api.SharkCurrencyComponent;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.utils.SerializationHelper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static currency.api.SharkCurrencyComponent.CURRENCY_FORMAT;

public class GroupInvitation {
    private final SharkGroupDocument document;
    private final String message;
    private final CharSequence uri;
    private final ASAPPeer asapPeer;
    private final SharkPKIComponent sharkPKIComponent;

    public GroupInvitation(ASAPPeer asapPeer,
                           CharSequence uri,
                           String message,
                           SharkGroupDocument document,
                           SharkPKIComponent sharkPKIComponent) {
        this.asapPeer = asapPeer;
        this.uri = uri;
        this.message = message;
        this.document = document;
        this.sharkPKIComponent = sharkPKIComponent;
    }

    // UI-Team you may use those methods
    public void accept() {
        try {
            byte[] signature = ASAPCryptoAlgorithms
                    .sign(this.document.getGroupId(), this.sharkPKIComponent.getASAPKeyStore());
            this.document.addMember(this.asapPeer.getPeerID(),signature);
            ASAPStorage storage = this.asapPeer.getASAPStorage(CURRENCY_FORMAT);
            storage.createChannel(this.uri);
            byte[] serializedDocument = this.document.toSaveByte();
            storage.add(this.uri, serializedDocument);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream daos = new DataOutputStream(baos);
            daos.writeUTF("ACCEPTED");
            daos.writeUTF(this.asapPeer.getPeerID().toString());
            daos.writeInt(signature.length);
            daos.write(signature);

            byte[] responseContent = baos.toByteArray();
            this.asapPeer.sendASAPMessage(CURRENCY_FORMAT,
                    uri + "//response",
                    responseContent);

            System.out.println("DEBUG: erfolgreich dokument gespeichert, id: "
                    + this.document.getGroupId());



        } catch (Exception e) {
            System.err.println("Fehler beim Akzeptieren: " + e.getMessage());
        }
    }

    public void decline() {
        try {
            this.sendResponse("DECLINED");
        } catch (Exception e) {
            System.err.println("Fehler beim Ablehnen: " + e.getMessage());
        }
    }

    private void sendResponse(String status) throws IOException, ASAPException {
        String responseUri = this.uri.toString() + "//response"; // answer on new Uri btw
        byte[] content = SerializationHelper.str2bytes(status);
        this.asapPeer.sendASAPMessage(CURRENCY_FORMAT, responseUri, content);
    }

    public String getMessage() { return message; }
    public SharkGroupDocument getDocument() { return document; }
}
