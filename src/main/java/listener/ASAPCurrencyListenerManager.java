package listener;

/**
 * Interface for managing currency listeners in ASAP Currency environment.
 */
public interface ASAPCurrencyListenerManager {

    /**
     * Add listener for receiving currency events in ASAP Currency environment.
     * @param listener listener which is to be added
     */
    void addASAPCurrencyListener(String format, ASAPCurrencyListener listener);

    /**
     * Remove currency listener.
     * @param listener listener which is to be removed
     */
    void removeASAPCurrencyListener(String format, ASAPCurrencyListener listener);
}
