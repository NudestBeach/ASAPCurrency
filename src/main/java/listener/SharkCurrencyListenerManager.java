package listener;


import Group.SharkGroupDocument;
import currency.api.SharkCurrencyComponent;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.listenermanager.GenericListenerImplementation;
import net.sharksystem.asap.listenermanager.GenericNotifier;

import java.io.IOException;
import java.io.*;

public class SharkCurrencyListenerManager extends GenericListenerImplementation<SharkCurrencyListener> {

    public void addSharkCurrencyListener(SharkCurrencyListener listener) {
        this.addListener(listener);
    }

    public void removeSharkCurrencyListener(SharkCurrencyListener listener) {
        this.removeListener(listener);
    }

    public void receivedInvite(ASAPMessages message, String sender) throws IOException, ASAPException {
        try {
            byte[] inviteData = message.getMessage(0, false);

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

            System.out.println("DEBUG: Parsed invite from " + sender);
            System.out.println("  - Currency: " + sharkGroupDocument.getAssignedCurrency().getCurrencyName());
            System.out.println("  - Message: " + (optionalMessage != null ? optionalMessage : "(none)"));
            this.notifyInviteReceived(sharkGroupDocument, sender, optionalMessage);
        } catch (Exception e) {
            System.err.println("ERROR parsing invite: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to parse invite", e);
        }
    }

    private void notifyInviteReceived(SharkGroupDocument sharkGroupDocument, String sender, String message) {
        InviteReceivedNotifier notifier = new InviteReceivedNotifier(sharkGroupDocument, sender, message);
        this.notifyAll(notifier, false);
    }

    public void acceptInvite(SharkGroupDocument sharkGroupDocument) {
        this.notifyInviteAccepted(sharkGroupDocument);
    }

    public void declineInvite(SharkGroupDocument sharkGroupDocument) {
        this.notifyInviteDeclined(sharkGroupDocument);
    }

    private void notifyInviteAccepted(SharkGroupDocument sharkGroupDocument) {
        InviteAcceptedNotifier notifier = new InviteAcceptedNotifier(sharkGroupDocument);
        this.notifyAll(notifier, false);
    }

    private void notifyInviteDeclined(SharkGroupDocument sharkGroupDocument) {
        InviteDeclinedNotifier notifier = new InviteDeclinedNotifier(sharkGroupDocument);
        this.notifyAll(notifier, false);
    }

    private class InviteReceivedNotifier implements GenericNotifier<SharkCurrencyListener> {
        private final SharkGroupDocument sharkGroupDocument;
        private final String sender;
        private final String message;

        public InviteReceivedNotifier(SharkGroupDocument sharkGroupDocument, String sender, String message) {
            this.sharkGroupDocument = sharkGroupDocument;
            this.sender = sender;
            this.message = message;
        }

        @Override
        public void doNotify(SharkCurrencyListener listener) {
            listener.onInviteReceived(this.sharkGroupDocument, this.sender, this.message);
        }
    }

    private class InviteAcceptedNotifier implements GenericNotifier<SharkCurrencyListener> {
        private final SharkGroupDocument sharkGroupDocument;

        public InviteAcceptedNotifier(SharkGroupDocument sharkGroupDocument) {
            this.sharkGroupDocument = sharkGroupDocument;
        }

        @Override
        public void doNotify(SharkCurrencyListener listener) {
            listener.onInviteAccepted(this.sharkGroupDocument);
        }
    }

    private class InviteDeclinedNotifier implements GenericNotifier<SharkCurrencyListener> {
        private final SharkGroupDocument sharkGroupDocument;

        public InviteDeclinedNotifier(SharkGroupDocument sharkGroupDocument) {
            this.sharkGroupDocument = sharkGroupDocument;
        }

        @Override
        public void doNotify(SharkCurrencyListener listener) {
            listener.onInviteDeclined(this.sharkGroupDocument);
        }
    }

    private class SharkCurrencyNotiReceivedNotifier implements GenericNotifier<SharkCurrencyListener> {
        private final CharSequence uri;

        public SharkCurrencyNotiReceivedNotifier(CharSequence uri) {
            this.uri = uri;
        }

        @Override
        public void doNotify(SharkCurrencyListener listener) {
            listener.handleSharkCurrencyNotification(this.uri);
        }
    }

}
