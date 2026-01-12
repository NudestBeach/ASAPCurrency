package currency.api;


import currency.classes.Currency;
import exepections.ASAPCurrencyException;
import net.sharksystem.ASAPFormats;
import net.sharksystem.SharkComponent;

import java.util.ArrayList;


/**
 * A SharkCurrency is a local trust based currency System for SharkPeers.
 * It can be used to create open whitelisted groups between SharkPeers to exchange currency.
 * A Group is always created by one SharkPeer and includes the creation of a new local currency.
 * The currency is always bound to the group and can only be exchanged between its members.
 *
 */
@ASAPFormats(formats = {SharkCurrencyComponent.CURRENCY_FORMAT})
public interface SharkCurrencyComponent extends SharkComponent {

    /**
     * Shark-Currency URI format
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
    void establishGroup(Currency currency, ArrayList<CharSequence> whitelisted, boolean encrypted, boolean balanceVisible)
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
     * Establishes a new currency group with specific configuration.
     * * Mapping to ASAP concepts:
     * - This version of the method offers the possibility to invite initial members
     * - This creates an ASAP Channel with the URI based on currency.getName().
     * - If 'whitelisted' is pass, this indicates a Closed Channel scenario.
     * - If 'encrypted' is true, the channel messages should be encrypted (requires exchange of keys).
     * * @param currency       The currency object containing name and metadata.
     *
     * @param inviteMembers  List of group members who will automatically be invited
     * @param whitelisted    It stands the different peers, which are allowed to communicate with each other
     * @param encrypted      If true, the communication within this group will be encrypted.
     * @param balanceVisible If true, members are allowed to see the balances of others (application logic).
     * @throws ASAPCurrencyException If the group/channel cannot be established.
     */
    void establishGroup(ArrayList<CharSequence> inviteMembers, Currency currency, ArrayList<CharSequence> whitelisted, boolean encrypted, boolean balanceVisible)
            throws ASAPCurrencyException;

    /**
     * Establishes a new currency group with specific configuration.
     * * Mapping to ASAP concepts:
     * - This version of the method offers the possibility to invite initial members
     * - This creates an ASAP Channel with the URI based on currency.getName().
     * - If 'encrypted' is true, the channel messages should be encrypted (requires exchange of keys).
     * * @param currency       The currency object containing name and metadata.
     *
     * @param inviteMembers  List of group members who will automatically be invited
     * @param encrypted      If true, the communication within this group will be encrypted.
     * @param balanceVisible If true, members are allowed to see the balances of others (application logic).
     * @throws ASAPCurrencyException If the group/channel cannot be established.
     */
    void establishGroup(ArrayList<CharSequence> inviteMembers, Currency currency, boolean encrypted, boolean balanceVisible)
            throws ASAPCurrencyException;

    /**
     * It is used for to Invite Members to a Group which has no invited Members as a list.
     * You can send Members one by one or multiple Members at once.
     * @param content It's the Groupdocument what you want to send
     * @param uri URI of the group you want to invite
     * @param peerId ID of the peer you want to invite
     *
     */
    void invitePeerToGroup(byte[] content, CharSequence uri, CharSequence peerId);

    /**
     * Sends a specific amount of Currency to another peer.
     * Creates a transaction, serializes it, signs it and adds it to the ASAP channel.
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