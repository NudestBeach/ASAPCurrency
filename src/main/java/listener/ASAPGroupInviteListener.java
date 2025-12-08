package listener;

import Group.SharkGroupDocument;
import exepections.WrongDestinationException;

public interface ASAPGroupInviteListener {

    /**
     * If you get via Gossip a message that you can join a Group this method will be called.
     * @param currencyID The group you want to join
     * @param groupDocument The group document with Metadata
     * @throws WrongDestinationException If the message does not belong to you.
     * TODO: SharkInviteMessage mit dem Nötigsten
     */
    void onGroupInviteReceived(String currencyID, SharkGroupDocument groupDocument) throws WrongDestinationException;
}
