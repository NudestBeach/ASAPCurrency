package currency.classes;

import java.io.Serializable;

/**
 * Represents a financial obligation or asset transfer within the network.
 * Based on the provided UML specification.
 */
public interface Promise {


    /**
     * Unique identifier of the promise.
     */
    byte[] getPromiseID();

    /**
     * Identifies the ASAP Channel / Group where this promise is valid.
     */
    byte[] getGroupIDOfPromise();

    /**
     * The ID (e.g., Public Key or PeerID) of the entity RECEIVING the value.
     */
    String getCreditorID();

    /**
     * The ID of the entity ISSUING the promise (the one who owes).
     */
    String getDebitorID();

    /**
     * Cryptographic signature of the debitor validating the promise.
     */
    byte[] getSignatureDebitor();

    /**
     * Cryptographic signature of the creditor (optional, usually for acceptance/settlement).
     * TODO: PKI macht schon den ganzen Spaß
     */
    String getSignatureCreditor();

    /**
     * The numeric amount of the promise.
     * Note: Typically integer based (e.g., Cents/Satoshis) to avoid floating point errors.
     */
    int getValue();

    /**
     * The definition of the currency (The "Taler" object).
     * Only reference will be gossiped
     */
    Currency getReferenceValue();

    /**
     * Returns the current state of the signing process.
     * TODO: sinnvoll für eine Blackbox?
     */
    Signings getPromiseState();

}