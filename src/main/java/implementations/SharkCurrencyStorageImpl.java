package implementations;

import currency.classes.SharkPromise;
import currency.storage.SharkCurrencyStorage;
import exepections.SharkCurrencyException;
import group.SharkGroupDocument;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SharkCurrencyStorageImpl implements SharkCurrencyStorage {

    private final List<SharkPromise> sharkPromiseStore = new ArrayList<>();
    private final Map<String, SharkGroupDocument> pendingInvites = new HashMap<>();
    private final Map<String, SharkGroupDocument> groupDocuments = new HashMap<>();

    @Override
    public void saveGroupDocument(byte[] groupId, SharkGroupDocument doc) {
        this.groupDocuments.put(toKey(groupId), doc);
    }

    @Override
    public SharkGroupDocument getGroupDocument(byte[] groupId) throws SharkCurrencyException {
        if(this.groupDocuments.containsKey(toKey(groupId))) {
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
    public void addSharkPromiseToStorage(SharkPromise promise) {
        this.sharkPromiseStore.add(promise);
    }

    @Override
    public void removeSharkPromiseFromStorage(CharSequence promiseId) {
        this.sharkPromiseStore.removeIf(promise ->
                promise.getPromiseID().toString().equals(promiseId.toString()));
    }

    @Override
    public SharkPromise getSharkPromiseFromStorage(CharSequence promiseId) {
        return this.sharkPromiseStore.stream()
                .filter(promise ->
                        promise.getPromiseID().toString().equals(promiseId.toString()))
                .findFirst()
                .orElse(null);
    }

    //this method is needed because of hashingf purposes
    //two identical byte[] array
    private String toKey(byte[] groupId) {
        return Base64.getEncoder().encodeToString(groupId);
    }
}
