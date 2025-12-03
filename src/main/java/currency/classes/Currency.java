package currency.classes;

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
     * Returns the ID of the creator of the currency.
     * @return the ID of the creator of the currency
     */
    byte[] getCreatorId();

    /**
     * Returns whether the currency has a global limit.
     * @return true if the currency has a global limit, false otherwise
     */
    Boolean hasGlobalLimit();

    /**
     * Returns whether the currency is centralized.
     * @return true if the currency is centralized, false otherwise
     */
    Boolean isCentralized();

}
