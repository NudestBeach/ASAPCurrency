package listener;

import currency.classes.Currency;
import currency.classes.Promise;
import net.sharksystem.asap.ASAPException;
import java.util.List;

public interface ASAPCurrencyManager {

    // ==========================================
    // Group / Channel Management (ASAP Layer)
    // ==========================================

    /**
     * Establishes a new currency group (ASAP Channel).
     * Maps the Currency to a URI and configures the channel properties.
     */
    void establishGroup(Currency currency, Boolean whitelisted, Boolean encrypted, Boolean balanceVisible)
            throws ASAPException;

    /**
     * Joins an existing currency group by subscribing to its URI.
     */
    void joinCurrencyGroup(String currencyName) throws ASAPException;


    // ==========================================
    // Promise Lifecycle (SharkPromisesComponent)
    // ==========================================

    /**
     * Factory method: Creates a new, local Promise object.
     * Initial state is UNSIGNED.
     * * @param value The amount (e.g., 100 Taler).
     * @param currency The currency reference.
     * @param recipientId The PeerID of the Creditor.
     * @return The new Promise object structure.
     */
    Promise createPromise(int value, Currency currency, String recipientId);

    /**
     * Cryptographically signs the promise with the local user's private key.
     * Updates the state from UNSIGNED to SIGNED_BY_DEBITOR.
     */
    void signPromise(Promise p) throws ASAPException;

    /**
     * Sends the signed promise into the ASAP network.
     * * Process:
     * 1. Serializes the Promise object.
     * 2. Identifies the correct ASAP Channel based on p.getGroupIDOfPromise().
     * 3. Calls asapEngine.add(uri, bytes).
     */
    void sendPromise(Promise p) throws ASAPException;

    /**
     * Transfers a received promise to a third party (Endorsement).
     * This essentially changes the CreditorID and requires a new signature chain.
     */
    void transferPromise(Promise p, String newCreditorId) throws ASAPException;


    // ==========================================
    // Access & Listeners
    // ==========================================

    /**
     * Aggregates valid promises where user is Creditor minus promises where user is Debitor.
     */
    int getBalance(String currencyName) throws ASAPException;

    /**
     * Returns all Promises stored in the ASAP storage for this currency.
     */
    List<Promise> getHistory(String currencyName) throws ASAPException;

    void addCurrencyListener(ASAPCurrencyListener listener);
}