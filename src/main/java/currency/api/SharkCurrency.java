package currency.api;


import currency.classes.Currency;
import currency.classes.Promise;
import listener.ASAPCurrencyListener;
import net.sharksystem.asap.ASAPException;

import java.util.List;

/**
 * A SharkCurrency is a local trust based currency System for SharkPeers. It can be used to create whitelisted or public Group between SharkPeers to exchange currency.
 * A Group is always created by one SharkPeer and includes the creation of a new currency. The currency is always bound to the group and can only be exchanged between the members.
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
     * @param whitelisted    If true, access is restricted to a specific list of peers (managed separately).
     * @param encrypted      If true, the communication within this group will be encrypted.
     * @param balanceVisible If true, members are allowed to see the balances of others (application logic).
     * @throws ASAPException If the group/channel cannot be established.
     */
    void establishGroup(Currency currency, Boolean whitelisted, Boolean encrypted, Boolean balanceVisible)
            throws ASAPException;

    /**
     * Joins an existing public currency group.
     * Signals interest in the specific URI to the ASAP engine.
     *
     * @param currencyName The name of the currency to join.
     * @throws ASAPException If the channel cannot be accessed.
     */
    void joinCurrencyGroup(String currencyName) throws ASAPException;

    /**
     * Sends a specific amount of currency to another peer.
     * Creates a transaction, serializes it, and adds it to the ASAP channel.
     *
     * @param currencyName The name of the currency group.
     * @param recipientId  The ASAP Peer ID of the recipient.
     * @param amount       The amount to transfer.
     * @param memo         An optional note for the transaction.
     * @throws ASAPException If the message cannot be sent.
     */
    void sendCurrency(String currencyName, CharSequence recipientId, double amount, String memo)
            throws ASAPException;

    /**
     * Calculates the current balance for the local user in the specified currency.
     *
     * @param currencyName The name of the currency.
     * @return The current balance.
     * @throws ASAPException If the history cannot be read.
     */
    double getBalance(String currencyName) throws ASAPException;

    /**
     * TODO Does this really matter
     * Returns the transaction history for a specific currency.
     * * @param currencyName The name of the currency.
     *
     * @return List of past transactions.
     * @throws ASAPException
     */
    List<Promise> getHistory(String currencyName) throws ASAPException;

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


    /**
     * OWNER-ONLY: Adds a user to a private/encrypted group.
     * Updates the internal recipient list (whitelist) of the channel.
     *@param currencyName The name of the group.
     *
     * @param peerId The ID (Public Key) of the user to be authorized.
     * @throws ASAPException If the current user is not the owner or the channel is missing.
     */
    void addMemberToGroup(String currencyName, String peerId) throws ASAPException;




}