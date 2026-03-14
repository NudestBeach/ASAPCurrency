package currency.classes;

import exepections.SharkPromiseException;

/**
 * Represents a financial obligation or asset transfer within the network.
 * Based on the provided UML specification.
 */
public interface SharkPromise {

    int SIGNED_MASK = 0x1;
    int ENCRYPTED_MASK = 0x2;

    // These URIs will tell you(the listener) what to do
    String SHARK_PROMISE_OVERALL_FORMAT = "//shark-promise//";
    String SHARK_PROMISE_ASK_FOR_SIGNATURE_AS_CRED = "signAsCreditor";
    String SHARK_PROMISE_ASK_FOR_SIGNATURE_AS_DEB = "signAsDebitor";

    /**
     * Unique identifier of the promise.
     */
    CharSequence getPromiseID();

    /**
     * Identifies the ASAP Channel / Group where this promise is valid.
     */
    byte[] getGroupIDOfPromise();

    /**
     * The ID (e.g., Public Key or PeerID) of the entity RECEIVING the value.
     */
    CharSequence getCreditorID();

    /**
     * The ID of the entity ISSUING the promise (the one who owes).
     */
    CharSequence getDebtorID();


    /**
     * The numeric amount of the promise.
     * Note: Typically integer based (e.g., Cents/Satoshis) to avoid floating point errors.
     */
    int getAmount();

    /**
     * The definition of the currency (The "Taler" object).
     * Only reference will be gossiped
     *
     * @return Currency - the currency object which is being referred to in this promise
     */
    SharkCurrency getReferenceValue();

    boolean allowedToChangeDebtor();
    boolean allowedToChangeCreditor();

    public byte[] getCreditorSignature();
    public byte[] getDebtorSignature();

    long getExpirationDate();

    SharkPromiseSignings getSigningStateOfPromise();

    void setSigningStateOfPromise(SharkPromiseSignings newState);

    void setAllowedToChangeDebtor(boolean on) throws SharkPromiseException;
    void setAllowedToChangeCreditor(boolean on) throws SharkPromiseException;

    void setCreditorSignature(byte[] signature);
    void setDebtorSignature(byte[] signature);
}