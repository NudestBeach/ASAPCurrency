package listener;

/**
 * Interface for managing group join listeners in ASAP Currency environment.
 */
public interface ASAPGroupJoinListenerManager {

    /**
     * Add listener for receiving group join events in ASAP Currency environment.
     * @param listener listener which is to be added
     */
    void addASAPGroupJoinListener(String format, ASAPGroupJoinListener listener);

    /**
     * Remove group join listener.
     * @param listener listener which is to be removed
     */
    void removeASAPGroupJoinListener(String format, ASAPGroupJoinListener listener);
}
