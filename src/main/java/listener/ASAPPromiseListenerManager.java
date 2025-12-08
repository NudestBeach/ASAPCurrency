package listener;

/**
 * Interface for managing promise listeners in ASAP Currency environment
 */
public interface ASAPPromiseListenerManager {

    /**
     * Add listener for receiving promises in ASAP Currency environment
     * @param listener listener which is to be added
     */
    void addASAPChannelContentChangedListener(String format, ASAPPromiseListener listener);

    /**
     * Remove promise listener
     * @param listener listener which is to be removed
     */
    void removeASAPChannelContentChangedListener(String format, ASAPPromiseListener listener);

}
