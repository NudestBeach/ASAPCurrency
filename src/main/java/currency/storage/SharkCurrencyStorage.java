package currency.storage;

import currency.classes.SharkPromise;
import group.SharkGroupDocument;

/**
 * This Interface provides methods for managing the storage within the
 * ASAPCurrency Application
 */
public interface SharkCurrencyStorage {

    //Group-Document Storage Methods
    void saveGroupDocument(byte[] groupId, SharkGroupDocument doc);
    SharkGroupDocument getGroupDocument(byte[] groupId);
    void addMemberToGroupDocument(byte[] groupId, CharSequence peerId, byte[] signature);

    //INVITE STORAGE METHODS
    void savePendingInvite(String currencyName, SharkGroupDocument doc, String optionalMessage);
    SharkGroupDocument getPendingInvite(String currencyName);
    void removePendingInvite(String currencyName);

    //PROMISE STORAGE METHODS
    void addSharkPromiseToStorage(SharkPromise promise);
    void removeSharkPromiseFromStorage(CharSequence promiseId);

}
