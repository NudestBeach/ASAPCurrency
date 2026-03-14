package currency.classes;

import exepections.SharkPromiseException;

import java.io.Serializable;
import java.util.Calendar;
import java.util.UUID;

public class SharkInMemoPromise implements SharkPromise, Serializable {

    private static final long serialVersionUID = 1L;

    private final int amount;
    private final SharkCurrency referenceValue;
    private final byte[] groupId;
    private boolean allowedToChangeDebtor, allowedToChangeCreditor;
    private long expirationDate;
    private CharSequence promiseID;
    private CharSequence debtorID, creditorID;
    private byte[] debtorSignature, creditorSignature;
    private SharkPromiseSignings promiseSignings;

    public SharkInMemoPromise(SharkPromise promise) {
        this(promise.getPromiseID(), promise.getAmount(),
                promise.getReferenceValue(), promise.getGroupIDOfPromise(),
                promise.allowedToChangeCreditor(), promise.allowedToChangeDebtor(),
                promise.getExpirationDate(), promise.getCreditorID(), promise.getDebtorID(),
                promise.getCreditorSignature(), promise.getDebtorSignature());
    }

    public SharkInMemoPromise(int amount,
                              SharkCurrency referenceValue,
                              byte[] groupId,
                              CharSequence creditorId,
                              CharSequence debtorId) {
        this(generatePromiseID(), amount,
                referenceValue, groupId,
                true, true,
                getDefaultExpirationDate(), creditorId, debtorId,
                null, null);
    }

    public SharkInMemoPromise(CharSequence promiseID,
                              int amount,
                              SharkCurrency referenceValue,
                              byte[] groupId,
                              boolean allowedToChangeCreditor,
                              boolean allowedToChangeDebtor,
                              long expirationDate,
                              CharSequence creditorID,
                              CharSequence debtorID,
                              byte[] creditorSignature,
                              byte[] debtorSignature) {
        this.promiseID=promiseID;
        this.amount=amount;
        this.referenceValue=referenceValue;
        this.groupId=groupId;
        this.allowedToChangeCreditor=allowedToChangeCreditor;
        this.allowedToChangeDebtor=allowedToChangeDebtor;
        this.expirationDate=expirationDate;
        this.creditorID=creditorID;
        this.debtorID=debtorID;
        this.creditorSignature=creditorSignature;
        this.debtorSignature=debtorSignature;
        this.promiseSignings=SharkPromiseSignings.UNSIGNED;
    }

    @Override
    public CharSequence getPromiseID() {
        return this.promiseID;
    }

    @Override
    public byte[] getGroupIDOfPromise() {
        return this.groupId;
    }

    @Override
    public CharSequence getCreditorID() {
        return this.creditorID;
    }

    @Override
    public CharSequence getDebtorID() {
        return this.debtorID;
    }

    @Override
    public int getAmount() {
        return this.amount;
    }

    @Override
    public SharkCurrency getReferenceValue() {
        return this.referenceValue;
    }

    @Override
    public boolean allowedToChangeCreditor() {
        return this.allowedToChangeCreditor;
    }

    @Override
    public boolean allowedToChangeDebtor() {
        return this.allowedToChangeDebtor;
    }

    @Override
    public long getExpirationDate() {
        return this.expirationDate;
    }

    @Override
    public byte[] getCreditorSignature() {
        return this.creditorSignature;
    }

    @Override
    public byte[] getDebtorSignature() {
        return this.debtorSignature;
    }

    @Override
    public void setAllowedToChangeCreditor(boolean allowed) throws SharkPromiseException {
        this.allowedToChangeCreditor=allowed;
    }

    @Override
    public void setAllowedToChangeDebtor(boolean allowed) throws SharkPromiseException {
        this.allowedToChangeDebtor=allowed;
    }

    @Override
    public void setCreditorSignature(byte[] signature) {
        this.creditorSignature=signature;
    }

    @Override
    public void setDebtorSignature(byte[] signature) {
        this.debtorSignature=signature;
    }

    @Override
    public SharkPromiseSignings getSigningStateOfPromise() {
        return this.promiseSignings;
    }

    @Override
    public void setSigningStateOfPromise(SharkPromiseSignings newState) {
        this.promiseSignings=newState;
    }

    /**
     * generates a new unique ID for this Promise
     * @return the unique UUID converted to a String
     */
    private static CharSequence generatePromiseID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    private static long getDefaultExpirationDate() {
        Calendar until = Calendar.getInstance();
        return until.getTimeInMillis();
    }
}
