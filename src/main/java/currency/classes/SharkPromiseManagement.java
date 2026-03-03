package currency.classes;

import exepections.SharkPromiseException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.crypto.ASAPKeyStore;
import net.sharksystem.pki.SharkPKIComponent;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SharkPromiseManagement {

    public static void signAsCreditor(ASAPKeyStore asapKeyStore, SharkPromise promise) throws SharkPromiseException, ASAPSecurityException, IOException {
        if (!asapKeyStore.getOwner().equals(promise.getCreditorID())) {
            throw new SharkPromiseException("The provided keyStore owner ("
                    + asapKeyStore.getOwner()
                    + ") doesn't match the creditor's id ("
                    + promise.getCreditorID() + ")");
        } else {
            promise.setDebtorSignature(signPromise(asapKeyStore, promise, true));
        }
    }

    public void signAsDebtor(ASAPKeyStore asapKeyStore, SharkPromise promise) throws SharkPromiseException, ASAPSecurityException, IOException {
        if (!asapKeyStore.getOwner().equals(promise.getDebtorID())) {
            throw new SharkPromiseException("The provided keyStore owner ("
                    + asapKeyStore.getOwner()
                    + ") doesn't match the debtor's id ("
                    + promise.getDebtorID() + ")");
        } else {
            promise.setDebtorSignature(signPromise(asapKeyStore, promise, true));
        }
    }

    private static byte[] signPromise(ASAPKeyStore asapKeyStore, SharkPromise promise, boolean signAsCreditor) throws ASAPSecurityException, IOException {
        Set<CharSequence> receiver = new HashSet<>();
        CharSequence sender;
        if (signAsCreditor) {
            sender = promise.getCreditorID();
            receiver.add(promise.getDebtorID());
        } else {
            sender = promise.getDebtorID();
            receiver.add(promise.getCreditorID());
        }
        return SharkPromiseSerializer.serializePromise(promise, sender, receiver, true, true, asapKeyStore, true, 1);
    }

}
