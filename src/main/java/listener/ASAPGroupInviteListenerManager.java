package listener;

/**
 * Interface for managing group invite listeners in ASAP Currency environment.
 */
public interface ASAPGroupInviteListenerManager {

    /**
     * Add listener for receiving group invite events in ASAP Currency environment.
     * @param listener listener which is to be added
     */
    void addASAPGroupInviteListener(String format,ASAPGroupInviteListener listener);

    /**
     * Remove group invite listener.
     * @param listener listener which is to be removed
     */
    void removeASAPGroupInviteListener(String format, ASAPGroupInviteListener listener);
}
