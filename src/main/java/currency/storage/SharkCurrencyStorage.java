package currency.storage;

import currency.classes.SharkPromise;
import exepections.SharkCurrencyException;
import group.SharkGroupDocument;

/**
 * This Interface provides methods for managing the storage within the
 * ASAPCurrency Application
 */
public interface SharkCurrencyStorage {

    //GROUP-DOCUMENT STORAGE METHODS
    void saveGroupDocument(byte[] groupId, SharkGroupDocument doc);
    SharkGroupDocument getGroupDocument(byte[] groupId) throws SharkCurrencyException;
    void addMemberToGroupDocument(byte[] groupId, CharSequence peerId, byte[] signature);
    boolean hasPendingInvites();
    int getPendingInviteSize();

    //INVITE STORAGE METHODS
    void savePendingInvite(String currencyName, SharkGroupDocument doc, String optionalMessage);
    SharkGroupDocument getPendingInvite(String currencyName);
    void removePendingInvite(String currencyName);

    //FULLY SIGNED PROMISE STORAGE METHODS
    void addSharkSignedPromiseToStorage(SharkPromise promise);
    void removeSharkSignedPromiseFromStorage(CharSequence promiseId);
    SharkPromise getSharkSignedPromiseFromStorage(CharSequence promiseId);

    //PENDING PROMISE STORAGE METHODS
    void addSharkPendingPromiseToStorage(SharkPromise promise);
    void removeSharkPendingPromiseFromStorage(CharSequence promiseId);
    void getallShrkPromiesesFromStorage();
    SharkPromise getSharkPendingPromiseFromStorage(CharSequence promiseId);

}
