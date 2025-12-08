package Group;

public class SharkGroupDocument {

    private byte[] groupId;
    private byte[] groupCreator;
    private Currency assignedCurrency;
    private boolean whitelisted; //Can we still leave it?
    private boolean encrypted;
    private boolean balanceVisible;
    private Object groupDocState; //TODO: we will make an enum
    private Map<byte[],byte[]> currentMembers; //Signature and ID so I think byte[] is wrong
    private ArrayList whitelistMember; //Maybe also remove
}
