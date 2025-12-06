package currency.classes;

import java.awt.*;

/**
 *
 */
public class LocalCurrency implements Currency {
    private boolean gloabalLimit;
    private List centralized = new List();
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
        return "";
    }

    @Override
    public String getSpecification() {
        return "";
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
