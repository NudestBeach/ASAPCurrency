package Group;

import currency.classes.Currency;
import currency.classes.GroupSignings;
import currency.classes.LocalCurrency;
import net.sharksystem.utils.SerializationHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.lang.Boolean.parseBoolean;

public class SharkGroupDocument {

    public static final String DOCUMENT_FORMAT = "application://x-asap-currency-group-document";
    private static final String EMPTY_PLACEHOLDER = "NULL";
    private static final String LIST_DELIMITER = ":::";
    private byte[] groupId;
    private CharSequence groupCreator;
    private Currency assignedCurrency;
    private ArrayList whitelistMember;
    private boolean encrypted;
    private boolean balanceVisible;
    private GroupSignings groupDocState;
    private Map<byte[],byte[]> currentMembers;

    // --- Konstruktor (Unverändert) ---
    public SharkGroupDocument(CharSequence groupCreator,
                              Currency assignedCurrency,
                              ArrayList whitelistMember,
                              boolean encrypted,
                              boolean balanceVisible,
                              GroupSignings groupDocState) {
        this.groupCreator = groupCreator;
        this.assignedCurrency = assignedCurrency;
        this.whitelistMember = whitelistMember;
        this.encrypted = encrypted;
        this.balanceVisible = balanceVisible;
        this.groupDocState = (groupDocState != null) ? groupDocState : GroupSignings.SIGNED_BY_NONE;
        this.groupId = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
    }

    // --- Serialisierung: Objekt -> byte[] ---

    /**
     * Converts the SharkGroupDocument object into a byte array for saving.
     * @return A byte array representation of the SharkGroupDocument.
     */
    public byte[] toSaveByte() throws IOException {
        List<CharSequence> parts = new ArrayList<>();

        // 1. GroupID (wird gespeichert, um die Struktur zu wahren)
        parts.add(bytesToCharSequenceSafe(this.groupId));

        // 2. GroupCreator
        parts.add(this.groupCreator);

        // 3. Currency
        if (this.assignedCurrency != null) {
            parts.add(charSequenceToSafe(this.assignedCurrency.getCurrencyName()));
            parts.add(charSequenceToSafe(this.assignedCurrency.getSpecification()));
        } else {
            parts.add(EMPTY_PLACEHOLDER);
            parts.add(EMPTY_PLACEHOLDER);
        }

        // 4. Liste serialisieren
        parts.add(serializeList(this.whitelistMember));

        // 5. Booleans & State
        parts.add(String.valueOf(this.encrypted));
        parts.add(String.valueOf(this.balanceVisible));
        parts.add(this.groupDocState.name());

        // String bauen und in Bytes konvertieren
        String serializedString = SerializationHelper.collection2String(parts);
        return SerializationHelper.str2bytes(serializedString);
    }

    // --- Deserialisierung: byte[] -> Objekt ---

    /**
     * Reconstructs a SharkGroupDocument object from a byte array.
     * @param data The byte array containing the serialized SharkGroupDocument data.
     * @return A new SharkGroupDocument object.
     */
    public static SharkGroupDocument fromByte(byte[] data) throws IOException {
        if (data == null) return null;

        // Bytes zurück in String wandeln
        String dataString = SerializationHelper.bytes2str(data);

        // String zerlegen
        List<CharSequence> parts = SerializationHelper.string2CharSequenceList(dataString);

        if (parts.size() < 8) {
            throw new IllegalArgumentException("Illegal Format for SharkGroupDocument: " + parts.size() + " Parts. Expected 8.");
        }

        int idx = 0;

        // 1. GroupID lesen und IGNORIEREN
        // Wir müssen den Token konsumieren, damit der Index stimmt,
        // aber der Konstruktor erzeugt eine neue ID, daher nutzen wir diesen Wert nicht.
        idx++;

        // 2. GroupCreator
        String cId = parts.get(idx++).toString();

        // 3. Currency
        String cName = safeCharSequenceToString(parts.get(idx++));
        String cSpec = safeCharSequenceToString(parts.get(idx++));
        Currency currency = new LocalCurrency(false, new ArrayList(), cName, cSpec);

        // 4. Whitelist
        CharSequence listData = parts.get(idx++);
        ArrayList<CharSequence> whitelist = deserializeList(listData);

        // 5. Booleans
        boolean enc = parseBoolean(parts.get(idx++));
        boolean bal = parseBoolean(parts.get(idx++));

        // 6. State
        String stateStr = parts.get(idx++).toString();
        GroupSignings state = GroupSignings.SIGNED_BY_NONE;
        try {
            state = GroupSignings.valueOf(stateStr);
        } catch (IllegalArgumentException e) {
            // ignore
        }

        return new SharkGroupDocument(cId, currency, whitelist, enc, bal, state);
    }

    // --- Helper Methoden ---

    private static CharSequence serializeList(List<CharSequence> list) {
        if (list == null || list.isEmpty()) return EMPTY_PLACEHOLDER;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(LIST_DELIMITER);
            }
        }
        return sb;
    }

    private static ArrayList<CharSequence> deserializeList(CharSequence data) {
        ArrayList<CharSequence> list = new ArrayList<>();
        if (data == null || data.toString().equals(EMPTY_PLACEHOLDER.toString())) {
            return list;
        }
        StringTokenizer tokenizer = new StringTokenizer(data.toString(), LIST_DELIMITER);
        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }
        return list;
    }

    private static boolean parseBoolean(CharSequence cs) {
        if (cs == null || cs.length() != 4) return false;
        return (cs.charAt(0) == 't' || cs.charAt(0) == 'T') &&
                (cs.charAt(1) == 'r' || cs.charAt(1) == 'R') &&
                (cs.charAt(2) == 'u' || cs.charAt(2) == 'U') &&
                (cs.charAt(3) == 'e' || cs.charAt(3) == 'E');
    }

    private static CharSequence bytesToCharSequenceSafe(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return EMPTY_PLACEHOLDER;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] safeCharSequenceToBytes(CharSequence cs) {
        if (cs == null || cs.toString().equals(EMPTY_PLACEHOLDER.toString())) return new byte[0];
        return cs.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static CharSequence charSequenceToSafe(CharSequence s) {
        if (s == null || s.length() == 0) return EMPTY_PLACEHOLDER;
        return s;
    }

    private static String safeCharSequenceToString(CharSequence s) {
        if (s == null || s.toString().equals(EMPTY_PLACEHOLDER.toString())) return "";
        return s.toString();
    }

    // --- Getter ---
    public byte[] getGroupId() { return groupId; }
    public CharSequence getGroupCreator() { return groupCreator; }
    public Currency getAssignedCurrency() { return assignedCurrency; }
    public boolean isEncrypted() { return encrypted; }
    public GroupSignings getGroupDocState() { return groupDocState; }
    public boolean isBalanceVisible() { return balanceVisible; }
    public Map<byte[], byte[]> getCurrentMembers() { return currentMembers; }
    public ArrayList getWhitelistMember() { return whitelistMember; }
}