package listener;

/**
 * This Interface should be used when a user has to be notified about an event.
 * Such an event can occur when for example a listener receives a new message
 *
 * @param <L> all type of

 */
public interface GenericNotifier <L>{

    /**
     * Generic Method to notify if something happens
     * @param var1 this is the listener, which should be notified
     */
    void doNotify(L var1);

}
