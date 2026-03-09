package currency.api;


import currency.classes.SharkCurrency;
import currency.storage.SharkCurrencyStorage;
import exepections.SharkCurrencyException;
import exepections.SharkPromiseException;
import listener.SharkCurrencyListener;
import listener.SharkCurrencyListenerManagerNEW;
import listener.SharkCurrencyListenerNEW;
import net.sharksystem.ASAPFormats;
import net.sharksystem.SharkComponent;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.ASAPStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;


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
    public final String INVITE_CHANNEL_URI = "//group-document//invite";
    public final String NEW_MEMBER_URI = "//group-document//new-member";

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
     * @throws SharkCurrencyException If the group/channel cannot be established.
     * @return the groupId
     */
    byte[] establishGroup(SharkCurrency currency, ArrayList<CharSequence> whitelisted, boolean encrypted, boolean balanceVisible)
            throws SharkCurrencyException;


    /**
     * Establishes a new currency group with specific configuration.
     * * Mapping to ASAP concepts:
     * - This creates an ASAP Channel with the URI based on currency.getName().
     * - If 'encrypted' is true, the channel messages should be encrypted (requires exchange of keys).
     * * @param currency       The currency object containing name and metadata.
     *
     * @param encrypted      If true, the communication within this group will be encrypted.
     * @param balanceVisible If true, members are allowed to see the balances of others (application logic).
     * @throws SharkCurrencyException If the group/channel cannot be established.
     * @return the groupId
     */
    byte[] establishGroup(SharkCurrency currency, boolean encrypted, boolean balanceVisible)
            throws SharkCurrencyException;

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
     * @throws SharkCurrencyException If the group/channel cannot be established.
     * @return the groupId
     */
    byte[] establishGroup(ArrayList<CharSequence> inviteMembers, SharkCurrency currency, ArrayList<CharSequence> whitelisted, boolean encrypted, boolean balanceVisible)
            throws SharkCurrencyException;

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
     * @throws SharkCurrencyException If the group/channel cannot be established.
     * @return the groupId
     */
    byte[] establishGroup(ArrayList<CharSequence> inviteMembers, SharkCurrency currency, boolean encrypted, boolean balanceVisible)
            throws SharkCurrencyException;

    /**
     * It is used for to Invite Members to a Group which has no invited Members as a list.
     * You can send Members one by one or multiple Members at once.
     * @param groupId Id of the group
     * @param optionalMessage An optional message can be added to an invitation
     * @param peerId ID of the peer you want to invite
     *
     */
    void invitePeerToGroup(byte[] groupId, String optionalMessage, CharSequence peerId)
            throws SharkCurrencyException;

    /**
     * Sends a specific amount of Currency to another peer.
     * Creates a transaction, serializes it, signs it and adds it to the ASAP channel.
     * @param currencyName The name of the currency group.
     * @param sender ID of sender
     * @param receiver The ASAP Peer IDs of the recipients.
     * @param sign true if this promise should be signed by sender
     * @param encrypt true if this prmoise should be encrypted
     * @param uri the uri of this promise
     * @throws SharkCurrencyException If the Promise cannot be sent due to error.
     */
    void sendPromise(CharSequence currencyName, CharSequence sender, Set<CharSequence> receiver, boolean sign, boolean encrypt, CharSequence uri)
            throws SharkCurrencyException;

    /**
     * creates a SharkPromise object
     *
     * @param amount the amount of the currency
     * @param referenceValue a Shark Currency which this promise references
     * @param creditorId id of creditor
     * @param debtorId id of debitor
     * @param asCreditor is this bond created with you as the creditor true/false
     */
    void createPromise(int amount,
                       SharkCurrency referenceValue,
                       byte[] groupId,
                       CharSequence creditorId,
                       CharSequence debtorId,
                       boolean asCreditor) throws ASAPSecurityException, SharkPromiseException, IOException, SharkCurrencyException;


    /**
     * Calculates the current balance for the local user in the specified currency.
     *
     * @param currencyName The name of the currency.
     * @return The current balance.
     * @throws SharkCurrencyException If the history cannot be read.
     */
    int getBalance(CharSequence currencyName) throws SharkCurrencyException;

    /**
     * this will execute the acceptInvite() method from the listener but first
     * it signs the document which currency name is given in the parameter :D
     *
     * @param currencyName name of the currency for the group
     */
    void acceptInviteAndSign(CharSequence currencyName) throws SharkCurrencyException, ASAPSecurityException, ASAPException, IOException;

    /**
     * declines an invite to a group,
     * it will remove the invite form pending invites in storage
     *
     * @param currencyName name of the group currency you were invited from
     */
    void declineInvite(CharSequence currencyName);

    /**
     *
     * @return
     * @throws IOException
     * @throws ASAPException
     */
    ASAPStorage getASAPStorage() throws IOException, ASAPException;

    /**
     * returns the shark currency storage
     *
     * @return SharkCurrencyStorage of the Peer
     */
    SharkCurrencyStorage getSharkCurrencyStorage();

    /**
     * subscribes a listener to this component
     * @param listener the listener that will be subscribed
     */
    void subscribeSharkCurrencyListener(SharkCurrencyListenerNEW listener);
}