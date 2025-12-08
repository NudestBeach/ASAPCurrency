package listener;

import currency.classes.Promise;

public interface ASAPPromiseListener {

    /**
     * Called, when a new Promise comes in.
     * @param promiseUri
     * @param promise
     */
    void onPromiseReceived(CharSequence promiseUri, Promise promise);
}
