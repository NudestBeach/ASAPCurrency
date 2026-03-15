package implementations;

import currency.classes.SharkPromise;
import currency.storage.SharkCurrencyStorage;
import exepections.SharkCurrencyException;
import group.SharkGroupDocument;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SharkCurrencyStorageImpl implements SharkCurrencyStorage {

    private final Map<CharSequence, SharkPromise> sharkPromiseStorePending = new HashMap<>();
    private final Map<CharSequence, SharkPromise> sharkPromiseStoreSigned = new HashMap<>();
    private final Map<String, SharkGroupDocument> pendingInvites = new HashMap<>();
    private final Map<String, SharkGroupDocument> groupDocuments = new HashMap<>();

    @Override
    public void saveGroupDocument(byte[] groupId, SharkGroupDocument doc) {
        this.groupDocuments.put(toKey(groupId), doc);
    }

    @Override
    public SharkGroupDocument getGroupDocument(byte[] groupId) throws SharkCurrencyException {
        if (this.groupDocuments.containsKey(toKey(groupId))) {
            return this.groupDocuments.get(toKey(groupId));
        } else {
            throw new SharkCurrencyException("Document with ID: " + groupId + " not found in Storage");
        }
    }

    @Override
    public void addMemberToGroupDocument(byte[] groupId, CharSequence peerId, byte[] signature) {
        SharkGroupDocument doc = this.groupDocuments.get(toKey(groupId));
        if (doc == null) {
            System.err.println("DEBUG: No group document found for groupId");
            return;
        }
        doc.addMember(peerId, signature);
    }

    @Override
    public void savePendingInvite(String currencyName, SharkGroupDocument doc, String optionalMessage) {
        this.pendingInvites.put(currencyName, doc);
    }

    @Override
    public SharkGroupDocument getPendingInvite(String currencyName) {
        return this.pendingInvites.get(currencyName);
    }

    @Override
    public void removePendingInvite(String currencyName) {
        this.pendingInvites.remove(currencyName);
    }

    @Override
    public boolean hasPendingInvites() {
        return !this.pendingInvites.isEmpty();
    }

    @Override
    public void addSharkSignedPromiseToStorage(SharkPromise promise) {
        CharSequence promiseId = promise.getPromiseID();
        this.sharkPromiseStoreSigned.put(promiseId, promise);
    }

    @Override
    public void removeSharkSignedPromiseFromStorage(CharSequence promiseId) {
        this.sharkPromiseStoreSigned.remove(promiseId);
    }

    @Override
    public SharkPromise getSharkSignedPromiseFromStorage(CharSequence promiseId) {
        return this.sharkPromiseStoreSigned.get(promiseId);
    }

    public void addSharkPendingPromiseToStorage(SharkPromise promise) {
        CharSequence promiseId = promise.getPromiseID();
        this.sharkPromiseStorePending.put(promiseId, promise);
    }

    public void removeSharkPendingPromiseFromStorage(CharSequence promiseId) {
        this.sharkPromiseStorePending.remove(promiseId);
    }

    public SharkPromise getSharkPendingPromiseFromStorage(CharSequence promiseId) {
        return this.sharkPromiseStorePending.get(promiseId);
    }

    //this method is needed because of hashingf purposes
    //two identical byte[] array
    private String toKey(byte[] groupId) {
        return Base64.getEncoder().encodeToString(groupId);
    }

    public int getPendingInviteSize() {
        return this.pendingInvites.size();
    }
    @Override
    public void getallShrkPromiesesFromStorage(){
        sharkPromiseStorePending.keySet().stream().forEach(System.out::println);
    }

}
