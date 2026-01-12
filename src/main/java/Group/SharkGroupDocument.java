package Group;

import currency.classes.Currency;
import currency.classes.GroupSignings;
import currency.classes.LocalCurrency;
import exepections.ASAPCurrencyException;
import net.sharksystem.utils.SerializationHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SharkGroupDocument {

    public static final String DOCUMENT_FORMAT = "//group-document";
    private static final String EMPTY_PLACEHOLDER = "NULL";
    private static final String LIST_DELIMITER = ":::";
    private final byte[] groupId;
    private final CharSequence groupCreator;
    private final Currency assignedCurrency;
    private ArrayList<CharSequence> whitelistMember;
    private final boolean encrypted;
    private final boolean balanceVisible;
    private final GroupSignings groupDocState;
    private final Map<CharSequence,byte[]> currentMembers = new HashMap<>(); //<PeerId, Signature>

    /**
     * Public constructor setting a new GroupId
     */
    public SharkGroupDocument(CharSequence groupCreator,
                              Currency assignedCurrency,
                              ArrayList<CharSequence> whitelistMember,
                              boolean encrypted,
                              boolean balanceVisible,
                              GroupSignings groupDocState) {
        this.whitelistMember = (whitelistMember != null)
                ? new ArrayList<>(whitelistMember)
                : new ArrayList<>();

        if (groupCreator != null) {
            String creatorStr = groupCreator.toString();
            boolean found = this.whitelistMember.stream()
                    .anyMatch(m -> m.toString().equals(creatorStr));

            if (!found) {
                this.whitelistMember.add(groupCreator);
            }
        }
        this.groupCreator = groupCreator;
        this.assignedCurrency = assignedCurrency;
        this.whitelistMember = whitelistMember;
        this.encrypted = encrypted;
        this.balanceVisible = balanceVisible;
        this.groupDocState = (groupDocState != null) ? groupDocState : GroupSignings.SIGNED_BY_NONE;
        this.groupId = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * PRIVATE Constructor where we set the groupId
     */
    private SharkGroupDocument(byte[] groupId,CharSequence groupCreator,
                              Currency assignedCurrency,
                              ArrayList<CharSequence> whitelistMember,
                              boolean encrypted,
                              boolean balanceVisible,
                              GroupSignings groupDocState) {
        this.whitelistMember = (whitelistMember != null)
                ? new ArrayList<>(whitelistMember)
                : new ArrayList<>();

        if (groupCreator != null) {
            String creatorStr = groupCreator.toString();
            boolean found = this.whitelistMember.stream()
                    .anyMatch(m -> m.toString().equals(creatorStr));

            if (!found) {
                this.whitelistMember.add(groupCreator);
            }
        }
        this.groupCreator = groupCreator;
        this.assignedCurrency = assignedCurrency;
        this.whitelistMember = whitelistMember;
        this.encrypted = encrypted;
        this.balanceVisible = balanceVisible;
        this.groupDocState = (groupDocState != null) ? groupDocState : GroupSignings.SIGNED_BY_NONE;
        this.groupId = groupId;
    }

    public boolean addMember(CharSequence peerId, byte[] signature) {
        if(peerId == null || peerId.length()==0) return false;
        if(signature==null || signature.length==0) return false;
        if (this.currentMembers.containsKey(peerId)) {
            return false;
        }
        this.currentMembers.put(peerId, signature);
        return true;
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

        StringBuilder membersSb = new StringBuilder();
        for (Map.Entry<CharSequence, byte[]> entry : this.currentMembers.entrySet()) {
            membersSb.append(entry.getKey()).append("=")
                    .append(Base64.getEncoder().encodeToString(entry.getValue()))
                    .append(LIST_DELIMITER);
        }
        parts.add(membersSb.length() > 0 ? membersSb.toString() : EMPTY_PLACEHOLDER);

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
    public static SharkGroupDocument fromByte(byte[] data) throws IOException, ASAPCurrencyException {
        if (data == null) return null;

        // Bytes zurück in String wandeln
        String dataString = SerializationHelper.bytes2str(data);

        // String zerlegen
        List<CharSequence> parts = SerializationHelper.string2CharSequenceList(dataString);

        if (parts.size() < 9) {
            throw new IllegalArgumentException("Illegal Format for SharkGroupDocument: " + parts.size() + " Parts. Expected 8.");
        }

        int idx = 0;

        // 1. GroupID lesen und IGNORIEREN
        // Wir müssen den Token konsumieren, damit der Index stimmt,
        // aber der Konstruktor erzeugt eine neue ID, daher nutzen wir diesen Wert nicht.
        byte[] gId = safeCharSequenceToBytes(parts.get(idx++));

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
        SharkGroupDocument doc = new SharkGroupDocument(gId, cId, currency, whitelist, enc, bal, state);

        // 7. Member-Map wiederherstellen (Letzter Part)
        String membersData = parts.get(idx++).toString();
        if (!membersData.equals(EMPTY_PLACEHOLDER)) {
            StringTokenizer st = new StringTokenizer(membersData, LIST_DELIMITER);
            while (st.hasMoreTokens()) {
                String pair = st.nextToken();
                String[] splitPair = pair.split("=");
                if(splitPair.length == 2) {
                    doc.addMember(splitPair[0], Base64.getDecoder().decode(splitPair[1]));
                }
            }
        }

        return doc;
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
    public Map<CharSequence, byte[]> getCurrentMembers() { return currentMembers; }
    public ArrayList getWhitelistMember() { return whitelistMember; }
}