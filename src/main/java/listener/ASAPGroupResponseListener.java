package listener;

import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.utils.Log;
import net.sharksystem.utils.SerializationHelper;

import java.io.IOException;

public class ASAPGroupResponseListener implements SharkCurrencyListener {

    private void handleAcceptance(CharSequence groupUri) {
        System.out.println("Der Peer hat die Einladung für " + groupUri + " ANGENOMMEN.");
        // 1. Lade das lokale SharkGroupDocument für diese URI
        // 2. Füge den Peer endgültig zur Member-Liste hinzu
        // 3. Sende das aktualisierte Dokument (mit neuem Member) an alle
    }

    private void handleDecline(String groupUri) {
        System.out.println("Der Peer hat die Einladung für " + groupUri + " ABGELEHNT.");
    }

    @Override
    public void handleSharkCurrencyNotification(CharSequence uri) {
        System.out.println("TESTESTESTESTEST");
    }
}
