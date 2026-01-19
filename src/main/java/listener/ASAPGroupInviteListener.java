package listener;

import Group.SharkGroupDocument;
import exepections.ASAPCurrencyException;
import exepections.WrongDestinationException;
import net.sharksystem.utils.SerializationHelper;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class ASAPGroupInviteListener implements ASAPCurrencyListener {

    @Override
    public void handleSharkCurrencyNotification(CharSequence uri, byte[] fullContent) {
        if (uri.toString().startsWith(SharkGroupDocument.DOCUMENT_FORMAT)) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(fullContent);
                 DataInputStream dais = new DataInputStream(bais)) {

                String optionalMessage = dais.readUTF();
                System.out.println("DEBUG: the optional message ist: " + optionalMessage);

                int objectLength = dais.readInt();
                byte[] docBytes = new byte[objectLength];
                dais.readFully(docBytes);

                SharkGroupDocument doc = SharkGroupDocument.fromByte(docBytes);

                //weiterer Umgang mit den empfangenen Daten

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ASAPCurrencyException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
