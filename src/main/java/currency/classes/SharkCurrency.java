package currency.classes;

import exepections.ASAPCurrencyException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.utils.SerializationHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Currency interface represents a currency.
 * It can be implemented by classes that represent a specific currency.
 */
public interface Currency {

    /**
     * Returns the ID of the currency.
     * @return the ID of the currency
     */
    byte[] getCurrencyId();

    /**
     * Returns the name of the currency.
     * @return the name of the currency
     */
    String getCurrencyName();

    /**
     * Returns the specification of the currency.
     * @return the specification of the currency
     */
    String getSpecification();

    /**
     * Returns whether the currency has a global limit.
     * @return true if the currency has a global limit, false otherwise
     */
    Boolean hasGlobalLimit();

    /**
     * Turns the Currency object into a byte[] for serialization purposes
     * @return the byte series of this currency object
     */
    byte[] toByte() throws ASAPCurrencyException, IOException, ASAPException;

}
