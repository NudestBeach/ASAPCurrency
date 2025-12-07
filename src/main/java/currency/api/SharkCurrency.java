package currency.api;


import Group.SharkGroupDocument;
import currency.classes.Currency;
import currency.classes.Promise;
import listener.ASAPCurrencyListener;
import net.sharksystem.asap.ASAPException;
import java.util.ArrayList;


/**
 * A SharkCurrency is a local trust based currency System for SharkPeers. It can be used to create public or private (whitelisted) Groups between SharkPeers to exchange currency.
 * A Group is always created by one SharkPeer and includes the creation of a new local currency. The currency is always bound to the group and can only be exchanged between its members.
 *
 *
 */
public interface SharkCurrency {

    /**
     * URI for the App-format
     */
    public static final String CURRENCY_FORMAT = "application://x-asap-currency";

    /**
     * Establishes a new currency group with specific configuration.
     * * Mapping to ASAP concepts:
     * - This creates an ASAP Channel with the URI based on currency.getName().
     * - If 'whitelisted' is true, this indicates a Closed Channel scenario.
     * - If 'encrypted' is true, the channel messages should be encrypted (requires exchange of keys).
     * * @param currency       The currency object containing name and metadata.
     *
     * @param whitelisted    It stands the different peers, which are allowed to communicate with each other
     * @param encrypted      If true, the communication within this group will be encrypted.
     * @param balanceVisible If true, members are allowed to see the balances of others (application logic).
     * @throws ASAPException If the group/channel cannot be established.
     */
    void establishGroupWhitelisted(Currency currency, ArrayList whitelisted, boolean encrypted, boolean balanceVisible)
            throws ASAPException;


    /**
     * Establishes a new currency group with specific configuration.
     * * Mapping to ASAP concepts:
     * - This creates an ASAP Channel with the URI based on currency.getName().
     * - If 'encrypted' is true, the channel messages should be encrypted (requires exchange of keys).
     * * @param currency       The currency object containing name and metadata.
     *
     * @param encrypted      If true, the communication within this group will be encrypted.
     * @param balanceVisible If true, members are allowed to see the balances of others (application logic).
     * @throws ASAPException If the group/channel cannot be established.
     */
    void establishGroup(Currency currency, boolean encrypted, boolean balanceVisible)
            throws ASAPException;




    /**
     * Sends a specific amount of Currency to another peer.
     * Creates a transaction, serializes it, and adds it to the ASAP channel.
     *
     * @param currencyName The name of the currency group.
     * @param recipientId  The ASAP Peer ID of the recipient.
     * @param amount       The amount to transfer.
     * @param note         An optional note for the transaction.
     * @throws ASAPException If the message cannot be sent.
     */
    void sendPromise(String currencyName, CharSequence recipientId, int amount, String note)
            throws ASAPException;


    /**
     * Cryptographically signs the promise with the local user's private key. We are using the SharkPKI.
     * Updates the state from UNSIGNED to SIGNED_BY_DEBITOR.
     * @param p The Promise that must be signed
     */
    void signPromise(Promise p) throws ASAPException;

    /**
     * Cryptographically signs the Group Document with the local user's private key. We are using the SharkPKI.
     * Updates the state from SIGNED_BY_NONE to SIGNED_BY_SOME for an unsigned Group Document.
     * Reaches the state of SIGNED_BY_ALL for a whitelisted Group if all members signed.
     * An open Group can never reach the State of SIGNED_BY_ALL.
     * @param groupDocument The Promise that must be signed
     */
    void signGroupDocument(SharkGroupDocument groupDocument) throws ASAPException;

    /**
     * Calculates the current balance for the local user in the specified currency.
     *
     * @param currencyName The name of the currency.
     * @return The current balance.
     * @throws ASAPException If the history cannot be read.
     */
    double getBalance(String currencyName) throws ASAPException;


    /**
     * Registers a listener to receive updates about transactions and balance changes.
     * The listener is registered globally for the currency format.
     *
     * @param listener The listener implementation.
     */
    void addCurrencyListener(ASAPCurrencyListener listener);

    /**
     * Removes a previously registered listener.
     *
     * @param listener The listener to remove.
     */
    void removeCurrencyListener(ASAPCurrencyListener listener);


}