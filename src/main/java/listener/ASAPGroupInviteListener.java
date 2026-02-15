package listener;

import Group.SharkGroupDocument;
import exepections.ASAPCurrencyException;
import exepections.WrongDestinationException;
import net.sharksystem.utils.SerializationHelper;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class ASAPGroupInviteListener implements SharkCurrencyListener {

    @Override
    public void handleSharkCurrencyNotification(CharSequence uri) {

    }
}
