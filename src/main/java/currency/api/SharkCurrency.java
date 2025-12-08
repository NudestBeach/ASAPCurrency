package currency.api;


import Group.SharkGroupDocument;
import currency.classes.Currency;
import currency.classes.Promise;
import exepections.ASAPCurrencyException;
import listener.ASAPCurrencyListener;
import java.util.ArrayList;


/**
 * A SharkCurrency is a local trust based currency System for SharkPeers.
 * It can be used to create public or private (whitelisted) Groups between SharkPeers to exchange currency.
 * A Group is always created by one SharkPeer and includes the creation of a new local currency.
 * The currency is always bound to the group and can only be exchanged between its members.
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
     * - If 'whitelisted' is pass, this indicates a Closed Channel scenario.
     * - If 'encrypted' is true, the channel messages should be encrypted (requires exchange of keys).
     * * @param currency       The currency object containing name and metadata.
     *
     * @param whitelisted    It stands the different peers, which are allowed to communicate with each other
     * @param encrypted      If true, the communication within this group will be encrypted.
     * @param balanceVisible If true, members are allowed to see the balances of others (application logic).
     * @throws ASAPCurrencyException If the group/channel cannot be established.
     */
    void establishGroup(Currency currency, ArrayList whitelisted, boolean encrypted, boolean balanceVisible)
            throws ASAPCurrencyException;


    /**
     * Establishes a new currency group with specific configuration.
     * * Mapping to ASAP concepts:
     * - This creates an ASAP Channel with the URI based on currency.getName().
     * - If 'encrypted' is true, the channel messages should be encrypted (requires exchange of keys).
     * * @param currency       The currency object containing name and metadata.
     *
     * @param encrypted      If true, the communication within this group will be encrypted.
     * @param balanceVisible If true, members are allowed to see the balances of others (application logic).
     * @throws ASAPCurrencyException If the group/channel cannot be established.
     */
    void establishGroup(Currency currency, boolean encrypted, boolean balanceVisible)
            throws ASAPCurrencyException;


    /**
     * Sends a specific amount of Currency to another peer.
     * Creates a transaction, serializes it, signs it and adds it to the ASAP channel.
     *
     * @param currencyName The name of the currency group.
     * @param recipientId  The ASAP Peer ID of the recipient.
     * @param amount       The amount to transfer.
     * @param note         An optional note for the transaction.
     * @throws ASAPCurrencyException If the Promise cannot be sent due to error.
     */
    void sendPromise(CharSequence currencyName, CharSequence recipientId, int amount, CharSequence note)
            throws ASAPCurrencyException;


    /**
     * Calculates the current balance for the local user in the specified currency.
     *
     * @param currencyName The name of the currency.
     * @return The current balance.
     * @throws ASAPCurrencyException If the history cannot be read.
     */
    int getBalance(CharSequence currencyName) throws ASAPCurrencyException;

}