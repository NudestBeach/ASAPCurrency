package currency.classes;

import exepections.ASAPCurrencyException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.utils.SerializationHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 *
 */
public class LocalCurrency implements Currency {
    private boolean globalLimit;
    private String currencyName;
    private String specification;
    private  byte[] id;


    public LocalCurrency(boolean globalLimit, String currencyName, String specification) {
        this.globalLimit = globalLimit;
        this.currencyName = currencyName;
        this.specification = specification;
        this.id = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
    }

    private LocalCurrency(byte[] id, boolean globalLimit, String currencyName, String specification) {
        this.id = id;
        this.globalLimit = globalLimit;
        this.currencyName = currencyName;
        this.specification = specification;
    }

    public byte[] toByte() throws ASAPException, IOException {
        List<CharSequence> currencyVariables = new ArrayList<>(); //6

        currencyVariables.add(new String(this.id, StandardCharsets.UTF_8));
        currencyVariables.add(this.currencyName);
        currencyVariables.add(this.specification);
        currencyVariables.add(String.valueOf(this.globalLimit));

        if(currencyVariables.size()==4) {
            String serializedString = SerializationHelper.collection2String(currencyVariables);
            return SerializationHelper.str2bytes(serializedString);
        } else {
            throw new ASAPCurrencyException("Failure serializing currency to byte");
        }
    }

    public static LocalCurrency fromByte(byte[] data) throws IOException, ASAPCurrencyException {
        if (data == null) return null;

        String dataString = SerializationHelper.bytes2str(data);
        List<CharSequence> currencyVariables = SerializationHelper.string2CharSequenceList(dataString);

        if (currencyVariables.size() < 4) {
            throw new ASAPCurrencyException("Invalid currency format: expected 4 parts, got " + currencyVariables.size());
        }



        byte[] id = SerializationHelper.characterSequence2bytes(currencyVariables.get(0));
        String name = currencyVariables.get(1).toString();
        String spec = currencyVariables.get(2).toString();
        boolean limit = parseBoolean(currencyVariables.get(3));

        return new LocalCurrency(id, limit, name, spec);
    }

    private static boolean parseBoolean(CharSequence cs) {
        if (cs == null || cs.length() != 4) return false;
        return cs.toString().equalsIgnoreCase("true");
    }

    @Override
    public byte[] getCurrencyId() {
        return this.id;
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
    public Boolean hasGlobalLimit() {
        return this.globalLimit;
    }
}
