package currencyGroupTests;
import group.GroupSignings;
import group.SharkGroupDocument;
import currency.api.SharkCurrencyComponent;
import org.apache.commons.io.FileUtils;
import currency.classes.Currency;
import currency.classes.LocalCurrency;
import exepections.ASAPCurrencyException;

import net.sharksystem.asap.*;
import net.sharksystem.SharkException;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;

import net.sharksystem.pki.SharkPKIComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testHelper.AsapCurrencyTestHelper;


import java.io.File;
import java.io.IOException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CurrencyGroupTests extends AsapCurrencyTestHelper {

    public CurrencyGroupTests() {
        super(CurrencyGroupTests.class.getSimpleName());
    }

    @BeforeEach
    void setUp() {
        String testClassName = CurrencyGroupTests.class.getSimpleName();
        String[] peerNames = {ALICE_NAME, BOB_NAME, CLARA_NAME, DAVID_NAME};
        for (String peer : peerNames) {
            File peerFolder = new File("testResultsRootFolder/" + testClassName + "/" + peer);
            if (peerFolder.exists()) {
                try {
                    FileUtils.cleanDirectory(peerFolder);
                } catch (IOException ignored) {}
            }
        }
    }

    @AfterEach
    void tearDown() {
        stopPeerSafely(this.aliceSharkPeer);
        stopPeerSafely(this.bobSharkPeer);
        stopPeerSafely(this.claraSharkPeer);
        stopPeerSafely(this.davidSharkPeer);
    }

    @Test
    public void aliceCreatesAGroupWithLocalCurrency()
            throws SharkException, IOException {

        // 0. Setting up Alice Peer
        this.setUpScenarioEstablishCurrency_1_justAlice();

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerA";
        Currency dummyCurrency = new LocalCurrency(
                false,                // global limit
                currencyName.toString(),        // Name
                "A test Currency"               // Spec
        );

        // uri is: //group-document//AliceTalerA
        CharSequence groupUriA = SharkGroupDocument.DOCUMENT_FORMAT + currencyName;

        // 2. Alice creates a new Group using the created Currency
        this.aliceCurrencyComponent.establishGroup(dummyCurrency,
                new ArrayList<>(),
                false,
                true);
        SharkGroupDocument testDoc = this.aliceImpl.getSharkGroupDocument(currencyName);
        byte[] groupId = testDoc.getGroupId();
        byte[] aliceSignature = testDoc.getCurrentMembers().get(ALICE_ID);

        // 3. Checking results
        ASAPStorage aliceStorage = aliceSharkPeer.getASAPPeer()
                .getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT);
        Map<CharSequence, byte[]> members = testDoc.getCurrentMembers();
        boolean channelExists = aliceStorage.channelExists(groupUriA);
        boolean verified = ASAPCryptoAlgorithms.verify(
                groupId, //Content der signiert wurde
                aliceSignature,
                ALICE_ID,
                ((SharkPKIComponent) aliceSharkPeer
                        .getComponent(SharkPKIComponent.class))
                        .getASAPKeyStore());
        Assertions
                .assertEquals(ALICE_ID, testDoc.getGroupCreator());
        Assertions
                .assertTrue(channelExists, "Channel does not exist");
        Assertions
                .assertTrue(verified, "The Signature of Alice could not have been verified");
        Assertions.assertTrue(members.containsKey(ALICE_ID));
        Assertions.assertArrayEquals(aliceSignature, members.get(ALICE_ID),
                "The saved signature is different than the original one");

        // TODO: Check all the channel
        // 4. Do some cleaning
        aliceStorage.removeChannel(groupUriA);
        aliceStorage.removeChannel("//group-documentAliceTalerA");
        aliceStorage.removeChannel("AliceTalerA");
        System.out.println("DEBUG: all channels: " + aliceStorage.getChannelURIs());
    }

    @Test
    public void establishGroupWithMemberThatIsNotWhitelisted()
            throws SharkException {

        // 0. Setting up Alice Peer
        this.setUpScenarioEstablishCurrency_1_justAlice();

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerB";
        Currency dummyCurrency = new LocalCurrency(
                false,                // global limit
                currencyName.toString(),        // Name
                "A test Currency"               // Spec
        );

        // 2. Alice creates a new Group using the created Currency and adding no one to whitelist.
        // The document should add her as the creator automatically but will throw error because
        // Bob is not whitelisted
        ArrayList<CharSequence> membersToBeInvited = new ArrayList<>();
        membersToBeInvited.add(BOB_ID);

        // 3. Checking the result
        Exception exception
                = assertThrows(ASAPCurrencyException.class, () ->
                this.aliceCurrencyComponent.establishGroup(membersToBeInvited,
                        dummyCurrency,
                        new ArrayList<>(),
                        false,
                        true));
        String expectedMessage = "Can not invite peers that are not on the whitelist.";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }


    // WORK IN PROGRESS, hier noch nicht fertig
    @Test
    public void successfullGroupInviteSendAndReceived() throws SharkException, InterruptedException, IOException {

        // 0. Set up Alice and Bob
        this.setUpScenarioEstablishCurrency_2_BobAndAlice();

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerC";
        Currency dummyCurrency = new LocalCurrency(
                false,
                currencyName.toString(),
                "A test Currency"
        );

        // 2. Alice creates a new Group and whitelists Bob
        ArrayList<CharSequence> whitelist = new ArrayList<>();
        whitelist.add(BOB_ID);

        this.aliceCurrencyComponent.establishGroup(
                dummyCurrency,
                whitelist,
                false,
                true);

        //Fehler behoben, dass es die uri nicht gefunden hat weil wir zu schnell waren
        Thread.sleep(2000);

        // 3. Encounter including message exchange starts, Alice will send a group invite to Bob the builder
        this.aliceCurrencyComponent
                .invitePeerToGroup(currencyName, "Hi Bob, join my group!", BOB_ID);

        // 4. Encounter
        this.runEncounter(this.aliceSharkPeer, this.bobSharkPeer, true);
        Thread.sleep(2000);

        // 5.(Assertions)
        SharkGroupDocument aliceDoc = this.aliceImpl.getSharkGroupDocument(currencyName);
        SharkGroupDocument bobDoc = this.bobImpl.getSharkGroupDocument(currencyName);
        byte[] groupId = aliceDoc.getGroupId();
        byte[] aliceSignature = aliceDoc.getCurrentMembers().get(ALICE_ID);
        boolean verifiedAliceSig = ASAPCryptoAlgorithms.verify(
                groupId,
                aliceSignature,
                ALICE_ID,
                ((SharkPKIComponent) aliceSharkPeer
                        .getComponent(SharkPKIComponent.class))
                        .getASAPKeyStore());

        Assertions
                .assertNotNull(aliceDoc, "Bob document ist null.");
        Assertions
                .assertNotNull(bobDoc, "Bob document ist null.");
        Assertions
                .assertArrayEquals(groupId,
                        bobDoc.getGroupId(),
                        "Die GroupID bei Bob muss mit der von Alice übereinstimmen.");

        //we expect 1 member. Just alice because bob didn't do anything yet
        Assertions
                .assertEquals(1,
                        aliceDoc.getCurrentMembers().size());
        Assertions
                .assertEquals(1,
                        bobDoc.getCurrentMembers().size());
        //Alice signature has to be verified
        Assertions
                .assertTrue(verifiedAliceSig, "Alice Signatur ist ungültig");
    }


    @Test
    public void receiveInviteListenerAcceptedTest() throws SharkException, IOException, InterruptedException {
        // 0. Set up Alice and Bob
        this.setUpScenarioEstablishCurrency_2_BobAndAlice();

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerD";
        Currency dummyCurrency = new LocalCurrency(
                false,
                currencyName.toString(),
                "A test Currency"
        );

        // 2. Alice creates a new Group and whitelists Bob
        ArrayList<CharSequence> whitelist = new ArrayList<>();
        whitelist.add(BOB_ID);

        this.aliceCurrencyComponent.establishGroup(
                dummyCurrency,
                whitelist,
                false,
                true);

        //Fehler behoben, dass es die uri nicht gefunden hat weil wir zu schnell waren
        Thread.sleep(2000);

        // 3. Encounter including message exchange starts, Alice will send a group invite to Bob the builder
        this.aliceCurrencyComponent
                .invitePeerToGroup(currencyName, "Hi Bob, join my group!", BOB_ID);

        // 4. Encounter
        this.runEncounter(this.aliceSharkPeer, this.bobSharkPeer, true);

        //5. Bob will accept the invitation
        this.bobImpl.acceptInviteAndSign(currencyName);

        Thread.sleep(2000);
        this.runEncounter(this.bobSharkPeer,this.aliceSharkPeer,true);
        Thread.sleep(2000);

        // 6.(Assertions)
        SharkGroupDocument aliceDoc = this.aliceImpl.getSharkGroupDocument(currencyName);
        SharkGroupDocument bobDoc = this.bobImpl.getSharkGroupDocument(currencyName);
        byte[] groupId = aliceDoc.getGroupId();
        byte[] aliceSignature = bobDoc.getCurrentMembers().get(ALICE_ID);
        byte[] bobSignature = bobDoc.getCurrentMembers().get(BOB_ID);
        System.out.println("DEBUG: bob sig: " + bobSignature);
        boolean verifiedAliceSig = ASAPCryptoAlgorithms.verify(
                groupId,
                aliceSignature,
                ALICE_ID,
                ((SharkPKIComponent) aliceSharkPeer
                        .getComponent(SharkPKIComponent.class))
                        .getASAPKeyStore());
        boolean verifiedBobSig = ASAPCryptoAlgorithms.verify(
                groupId,
                bobSignature,
                BOB_ID,
                ((SharkPKIComponent) bobSharkPeer
                        .getComponent(SharkPKIComponent.class))
                        .getASAPKeyStore());

        Assertions
                .assertNotNull(bobDoc, "Bob document is null.");
        Assertions
                .assertArrayEquals(groupId,
                        bobDoc.getGroupId(),
                        "GroupId of Bobs document has to be the same as Alices .");
        Assertions
                .assertEquals(GroupSignings.SIGNED_BY_ALL,
                        bobDoc.getGroupDocState(),
                        "Bobs Group Document is not SIGNED_BY_ALL");

        //we expect 2 members each, alice and bob, so 4 in  total 0,1,2,3
        Assertions
                .assertEquals(2,
                        bobDoc.getCurrentMembers().size());
        Assertions
                .assertEquals(2,
                        aliceDoc.getCurrentMembers().size());

        Assertions
                .assertTrue(verifiedAliceSig, "Alice signature is not verified");
        Assertions
                .assertTrue(verifiedBobSig, "Bob signature is not verified");
    }


    @Test
    public void receiveInviteListenerDeniedTest() throws SharkException, IOException, InterruptedException {

        // 0. Set up Alice and Bob
        this.setUpScenarioEstablishCurrency_2_BobAndAlice();

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerF";
        Currency dummyCurrency = new LocalCurrency(
                false,
                currencyName.toString(),
                "A test Currency"
        );

        // 2. Alice creates a new Group and whitelists Bob
        ArrayList<CharSequence> whitelist = new ArrayList<>();
        whitelist.add(BOB_ID);

        this.aliceCurrencyComponent.establishGroup(
                dummyCurrency,
                whitelist,
                false,
                true);

        //Fehler behoben, dass es die uri nicht gefunden hat weil wir zu schnell waren
        Thread.sleep(2000);

        // 3. Encounter including message exchange starts, Alice will send a group invite to Bob the builder
        this.aliceCurrencyComponent
                .invitePeerToGroup(currencyName, "Hi Bob, join my group!", BOB_ID);

        // 4. Encounter
        this.runEncounter(this.aliceSharkPeer, this.bobSharkPeer, true);

        //5. Bob will decline the invitation
        this.bobImpl.declineInvite(this.bobImpl.getSharkGroupDocument(currencyName));

        //6. Assertions
        //Assertions.

    }


    @Test
    public void sendGroupInviteToMoreThanOnePersonAndAccept() throws SharkException, IOException, InterruptedException {
        this.setUpScenarioEstablishCurrency_4_DavidAndClaraAndBobAndAlice();

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerG";
        Currency dummyCurrency = new LocalCurrency(
                false,
                currencyName.toString(),
                "A test Currency"
        );

        // 2. Alice creates a new Group and whitelists Bob, Clara and David
        ArrayList<CharSequence> whitelist = new ArrayList<>();
        whitelist.add(BOB_ID);
        whitelist.add(CLARA_ID);
        whitelist.add(DAVID_ID);

        this.aliceCurrencyComponent.establishGroup(
                dummyCurrency,
                whitelist,
                false,
                true);

        // Zeit zum sicheren establishen der Gruppe
        Thread.sleep(2000);

        // 3. Encounter including message exchange starts, Alice will send a group invite to Bob, Clara and David the builder
        this.aliceCurrencyComponent
                .invitePeerToGroup(currencyName, "Hi Bob, join my group!", BOB_ID);
        this.aliceCurrencyComponent
                .invitePeerToGroup(currencyName, "Hi Clara, join my group!", CLARA_ID);
        this.aliceCurrencyComponent
                .invitePeerToGroup(currencyName, "Hi David, join my group!", DAVID_ID);

        // 4. Encounter
        this.runEncounter(this.aliceSharkPeer, this.bobSharkPeer, true);
        this.runEncounter(this.aliceSharkPeer, this.claraSharkPeer, true);
        this.runEncounter(this.aliceSharkPeer, this.davidSharkPeer, true);

        Thread.sleep(2000);

        // 5. Accept Invitation
        this.bobImpl.acceptInviteAndSign(currencyName);
        this.claraImpl.acceptInviteAndSign(currencyName);
        this.davidImpl.acceptInviteAndSign(currencyName);

        Thread.sleep(1000);
        this.runEncounter(this.bobSharkPeer, this.aliceSharkPeer, true);
        Thread.sleep(1000);
        this.runEncounter(this.claraSharkPeer, this.aliceSharkPeer, true);
        Thread.sleep(1000);
        this.runEncounter(this.davidSharkPeer, this.aliceSharkPeer, true);
        Thread.sleep(2000);

        // 6.(Assertions)
        SharkGroupDocument aliceDoc = this.aliceImpl.getSharkGroupDocument(currencyName);
        SharkGroupDocument bobDoc = this.bobImpl.getSharkGroupDocument(currencyName);
        SharkGroupDocument claraDoc = this.claraImpl.getSharkGroupDocument(currencyName);
        SharkGroupDocument davidDoc = this.davidImpl.getSharkGroupDocument(currencyName);
        byte[] groupId = aliceDoc.getGroupId();
        System.out.println("DEBUG: Alice doc members: " + aliceDoc.getCurrentMembers());

        Assertions.assertNotNull(bobDoc, "Bob document ist null.");
        Assertions.assertNotNull(claraDoc, "Clara document ist null.");
        Assertions.assertNotNull(davidDoc, "David document ist null.");

        Assertions.assertArrayEquals(groupId, bobDoc.getGroupId(), "Die GroupID bei Bob muss mit der von Alice übereinstimmen.");
        Assertions.assertArrayEquals(groupId, claraDoc.getGroupId(), "Die GroupID bei Clara muss mit der von Alice übereinstimmen.");
        Assertions.assertArrayEquals(groupId, davidDoc.getGroupId(), "Die GroupID bei David muss mit der von Alice übereinstimmen.");

        Assertions.assertEquals(GroupSignings.SIGNED_BY_SOME, aliceDoc.getGroupDocState(), "Alice Group Document is not SIGNED_BY_SOME");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_SOME, bobDoc.getGroupDocState(), "Bobs Group Document is not SIGNED_BY_SOME");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_SOME, claraDoc.getGroupDocState(), "Claras Group Document is not SIGNED_BY_SOME");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_SOME, davidDoc.getGroupDocState(), "Davids Group Document is not SIGNED_BY_SOME");

        Assertions
                .assertEquals(4,
                        aliceDoc.getCurrentMembers().size(),
                        "Alice docs member count is not correct");
        Assertions
                .assertEquals(4,
                        bobDoc.getCurrentMembers().size(),
                        "Bob docs member count is not correct");
        Assertions
                .assertEquals(4,
                        claraDoc.getCurrentMembers().size(),
                        "Clara docs member count is not correct");
        Assertions
                .assertEquals(4,
                        davidDoc.getCurrentMembers().size(),
                        "David docs member count is not correct");

        byte[] aliceSignature = aliceDoc.getCurrentMembers().get(ALICE_ID);
        byte[] bobSignature = bobDoc.getCurrentMembers().get(BOB_ID);
        byte[] claraSignature = claraDoc.getCurrentMembers().get(CLARA_ID);
        byte[] davidSignature = davidDoc.getCurrentMembers().get(DAVID_ID);

        boolean verifiedAliceSig = ASAPCryptoAlgorithms.verify(groupId, aliceSignature, ALICE_ID,
                ((SharkPKIComponent) aliceSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        boolean verifiedBobSig = ASAPCryptoAlgorithms.verify(groupId, bobSignature, BOB_ID,
                ((SharkPKIComponent) bobSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        boolean verifiedClaraSig = ASAPCryptoAlgorithms.verify(groupId, claraSignature, CLARA_ID,
                ((SharkPKIComponent) claraSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        boolean verifiedDavidSig = ASAPCryptoAlgorithms.verify(groupId, davidSignature, DAVID_ID,
                ((SharkPKIComponent) davidSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());

        Assertions.assertTrue(verifiedAliceSig, "Alice Signatur ist ungültig");
        Assertions.assertTrue(verifiedBobSig, "Bob Signatur ist ungültig");
        Assertions.assertTrue(verifiedClaraSig, "Clara Signatur ist ungültig");
        Assertions.assertTrue(verifiedDavidSig, "David Signatur ist ungültig");
    }


}