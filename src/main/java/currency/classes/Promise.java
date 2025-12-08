package currency.classes;

import java.io.Serializable;

/**
 * Represents a financial obligation or asset transfer within the network.
 * Based on the provided UML specification.
 */
public interface Promise {

    public static final String PROMISE_FORMAT = "application://x-asap-currency-promise";

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


    //TODO: PKI macht schon den ganzen Spaß


    /**
     * The numeric amount of the promise.
     * Note: Typically integer based (e.g., Cents/Satoshis) to avoid floating point errors.
     */
    int getValue();

    /**
     * The definition of the currency (The "Taler" object).
     * Only reference will be gossiped
     *
     * @return Currency - the currency object which is being referred to in this promise
     */
    Currency getReferenceValue();

}