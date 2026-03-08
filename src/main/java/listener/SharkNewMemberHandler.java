package listener;

import currency.storage.SharkCurrencyStorage;
import net.sharksystem.asap.ASAPChannel;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.pki.SharkPKIComponent;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class SharkNewMemberHandler implements SharkCurrencyMessageHandler{

    private final SharkCurrencyStorage sharkCurrencyStorage;

    public SharkNewMemberHandler(SharkCurrencyStorage sharkCurrencyStorage) {
        this.sharkCurrencyStorage = sharkCurrencyStorage;
    }

    @Override
    public void handle(CharSequence uri, ASAPStorage storage, SharkPKIComponent pki, CharSequence sender) {

        try {
            ASAPChannel newMemberChannel = storage.getChannel(uri);
            ASAPMessages messages = newMemberChannel.getMessages();
            System.out.println("DEBUG: received a new member message from: " + sender + " message size is: " + messages.size());

            //--------Read all data from the message ------------------------
            for (int i = 0; i < messages.size(); i++) {
                byte[] messageData = messages.getMessage(i, true);

                ByteArrayInputStream bais = new ByteArrayInputStream(messageData);
                DataInputStream dis = new DataInputStream(bais);
                String peerID = dis.readUTF();
                int groupIdLength = dis.readInt();
                byte[] groupId = new byte[groupIdLength];
                dis.readFully(groupId);
                int sigLength = dis.readInt();
                byte[] signature = new byte[sigLength];
                dis.readFully(signature);
                //adding the person to my doc
                this.sharkCurrencyStorage.addMemberToGroupDocument(groupId, peerID, signature);
            }
        } catch (IOException | ASAPException e) {
            throw new RuntimeException(e);
        }
    }
}
