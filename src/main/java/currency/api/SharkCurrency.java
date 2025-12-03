package currency.api;


import currency.classes.Currency;

/**
 * A SharkCurrency is a local trust based currency System for SharkPeers. It can be used to create whitelisted or public Group between SharkPeers to exchange currency.
 * A Group is always created by one SharkPeer and includes the creation of a new currency. The currency is always bound to the group and can only be exchanged between the members.
 *
 *
 */
public interface SharkCurrency{

    /**
     * Create a new Group with the given parameters
     */
    public void establishGroup(Currency currency, Boolean whitelisted, Boolean encrypted, Boolean balanceVisible);

    /**
     * Create a Promise with the given parameters
     */
    public void createAndGossipPromise();

    /**
     * TODO: talk about this shyt because we dont know yet,
     * Idea is that we persist a currency that we configured
     */
    public boolean saveCurrencyInmemory(Currenc);

}
