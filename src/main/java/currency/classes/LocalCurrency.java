package currency.classes;

import java.util.*;

/**
 *
 */
public class LocalCurrency implements Currency {
    private boolean gloabalLimit;
    private List centralized = new ArrayList();
    private String currencyName;
    private String specification;
    private  byte[] id = new byte[0];


    public LocalCurrency(boolean gloabalLimit, List centralized, String currencyName, String specification) {
        this.gloabalLimit = gloabalLimit;
        this.currencyName = currencyName;
        this.specification = specification;
        this.centralized = centralized;

    }

    @Override
    public byte[] getCurrencyId() {
        return new byte[0];
    }

    @Override
    public String getCurrencyName() {
        return this.currencyName;
    }

    @Override
    public String getSpecification() {
        return this.specification;
    }

    @Override
    public byte[] getCreatorId() {
        return new byte[0];
    }

    @Override
    public Boolean hasGlobalLimit() {
        return null;
    }

    @Override
    public Boolean isCentralized() {
        return null;
    }
}
