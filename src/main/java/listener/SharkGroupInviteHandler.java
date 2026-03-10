package listener;

import currency.storage.SharkCurrencyStorage;
import group.SharkGroupDocument;
import net.sharksystem.asap.ASAPChannel;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.pki.SharkPKIComponent;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;


public class SharkGroupInviteHandler implements SharkCurrencyMessageHandler {

    private final SharkCurrencyStorage sharkCurrencyStorage;

    public SharkGroupInviteHandler(SharkCurrencyStorage sharkCurrencyStorage) {
        this.sharkCurrencyStorage = sharkCurrencyStorage;
    }

    @Override
    public void handle(CharSequence uri, ASAPStorage storage, SharkPKIComponent pki, CharSequence sender) {
        try {
            ASAPMessages messages = storage.getChannel(uri).getMessages(false);
            if (messages.size() == 0) {
                System.err.println("DEBUG: No messages found in channel " + uri);
                return;
            }
            byte[] inviteData = messages.getMessage(messages.size() - 1, true);

            ByteArrayInputStream bais = new ByteArrayInputStream(inviteData);
            DataInputStream dais = new DataInputStream(bais);

            String optionalMessage = dais.readUTF();
            if (optionalMessage.isEmpty()) {
                optionalMessage = null;
            }

            int docLength = dais.readInt();
            byte[] docBytes = new byte[docLength];
            dais.readFully(docBytes);
            SharkGroupDocument sharkGroupDocument = SharkGroupDocument.fromByte(docBytes);

            sharkCurrencyStorage
                    .savePendingInvite(sharkGroupDocument
                            .getAssignedCurrency()
                            .getCurrencyName(), sharkGroupDocument, optionalMessage);

            System.out.println("DEBUG: Parsed invite from " + sender);
            System.out.println("  - Currency: " + sharkGroupDocument.getAssignedCurrency().getCurrencyName());
            System.out.println("  - Message: " + (optionalMessage != null ? optionalMessage : "(none)"));
        } catch (IOException | ASAPException e) {
            throw new RuntimeException(e);
        }
    }
}
