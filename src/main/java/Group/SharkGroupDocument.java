package Group;

public class SharkGroupDocument {

    public static final String DOCUMENT_FORMAT = "application://x-asap-currency-group-document";


    private byte[] groupId;
    private byte[] groupCreator;
    private Currency assignedCurrency;
    private boolean whitelisted;
    private boolean encrypted;
    private boolean balanceVisible;
    private Object groupDocState; //TODO: we will make an enum
    private Map<byte[],byte[]> currentMembers; //Signature and ID so I think byte[] is wrong
    private ArrayList whitelistMember; //Maybe also remove
}
