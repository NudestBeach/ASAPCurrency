package Group;

import currency.classes.Currency;
import currency.classes.LocalCurrency;
import exepections.ASAPCurrencyException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.utils.SerializationHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SharkGroupDocument {

    public static final String DOCUMENT_FORMAT = "//group-document//";
    private static final String EMPTY_PLACEHOLDER = "NULL";
    private static final String LIST_DELIMITER = ":::";
    private final byte[] groupId;
    private final CharSequence groupCreator;
    private final Currency assignedCurrency;
    private ArrayList<CharSequence> whitelistMember;
    private final boolean encrypted;
    private final boolean balanceVisible;
    private GroupSignings groupDocState;
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

    /**
     * Prüft ob der aktuelle Gruppendokument State aktuell ist
     * und aktuallisiert ihn bei Bedarf
     * TODO: updateGroupDocState benutzen
     */
    private void updateGroupDocState() {
        if (this.whitelistMember == null || this.whitelistMember.isEmpty()) return;

        boolean allSigned = true;
        for (CharSequence expetedMember : this.whitelistMember){
            boolean found = this.currentMembers.keySet().stream()
                    .anyMatch(member -> member.toString().equals(expetedMember.toString()));

            if (!found) {
                allSigned = false;
                break;
            }
        }
        if (allSigned){
            this.groupDocState = GroupSignings.SIGNED_BY_ALL;
        } else {
            this.groupDocState = GroupSignings.SIGNED_BY_SOME;
        }
    }

    // --- Serialisierung: Objekt -> byte[] ---

    /**
     * Converts the SharkGroupDocument object into a byte array for saving.
     * @return A byte array representation of the SharkGroupDocument.
     */
    public byte[] sharkDocumentToByte() throws IOException, ASAPException {
        List<CharSequence> documentVariables = new ArrayList<>();

        // 1. GroupID (wird gespeichert, um die Struktur zu wahren)
        documentVariables.add(bytesToCharSequenceSafe(this.groupId));

        // 2. GroupCreator
        documentVariables.add(this.groupCreator);

        // 3. Currency
        if (this.assignedCurrency != null) {
            byte[] currencyBytes = ((LocalCurrency)this.assignedCurrency).toByte();
            documentVariables.add(Base64.getEncoder().encodeToString(currencyBytes));
        } else {
            documentVariables.add(EMPTY_PLACEHOLDER);
        }

        // 4. Liste serialisieren
        documentVariables.add(serializeList(this.whitelistMember));

        // 5. Booleans & State
        documentVariables.add(String.valueOf(this.encrypted));
        documentVariables.add(String.valueOf(this.balanceVisible));
        documentVariables.add(this.groupDocState.name());

        StringBuilder membersSb = new StringBuilder();
        for (Map.Entry<CharSequence, byte[]> entry : this.currentMembers.entrySet()) {
            membersSb.append(entry.getKey()).append("=")
                    .append(Base64.getEncoder().encodeToString(entry.getValue()))
                    .append(LIST_DELIMITER);
        }
        documentVariables.add(membersSb.length() > 0 ? membersSb.toString() : EMPTY_PLACEHOLDER);

        // String bauen und in Bytes konvertieren
        String serializedString = SerializationHelper.collection2String(documentVariables);
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
        List<CharSequence> documentVariables = SerializationHelper.string2CharSequenceList(dataString);

        if (documentVariables.size() < 8) {
            throw new IllegalArgumentException("Illegal Format for SharkGroupDocument: " + documentVariables.size() + " Parts. Expected 8.");
        }

        int idx = 0;

        // 1. GroupID lesen und IGNORIEREN
        // Wir müssen den Token konsumieren, damit der Index stimmt,
        // aber der Konstruktor erzeugt eine neue ID, daher nutzen wir diesen Wert nicht.
        byte[] gId = safeCharSequenceToBytes(documentVariables.get(idx++));

        // 2. GroupCreator
        String cId = documentVariables.get(idx++).toString();

        // 3. Currency
        Currency currency = null;
        String currencyData = documentVariables.get(idx++).toString();
        if (!currencyData.equals(EMPTY_PLACEHOLDER)) {
            byte[] currencyBytes = Base64.getDecoder().decode(currencyData);
            currency = LocalCurrency.fromByte(currencyBytes);
        }

        // 4. Whitelist
        CharSequence listData = documentVariables.get(idx++);
        ArrayList<CharSequence> whitelist = deserializeList(listData);

        // 5. Booleans
        boolean enc = parseBoolean(documentVariables.get(idx++));
        boolean bal = parseBoolean(documentVariables.get(idx++));

        // 6. State
        String stateStr = documentVariables.get(idx++).toString();
        GroupSignings state = GroupSignings.SIGNED_BY_NONE;
        try {
            state = GroupSignings.valueOf(stateStr);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        SharkGroupDocument doc = new SharkGroupDocument(gId, cId, currency, whitelist, enc, bal, state);

        // 7. Member-Map wiederherstellen (Letzter Part)
        String membersData = documentVariables.get(idx++).toString();
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

        if(doc==null || doc.groupId.length<=0) {
            throw new ASAPCurrencyException("Error in group-document serialization");
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