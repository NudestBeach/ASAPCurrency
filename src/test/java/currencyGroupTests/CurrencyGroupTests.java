package currencyGroupTests;
import group.GroupSignings;
import group.SharkGroupDocument;
import currency.classes.SharkCurrency;
import currency.classes.SharkLocalCurrency;
import exepections.SharkCurrencyException;

import net.sharksystem.SharkException;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;

import net.sharksystem.pki.SharkPKIComponent;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.*;
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
    void setUp(TestInfo testInfo) {
        String testName = testInfo.getDisplayName()
                .replaceAll("[^a-zA-Z0-9]", "_");
        this.initSubRootFolder(
                CurrencyGroupTests.class.getSimpleName(),
                testName
        );
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
        SharkCurrency dummyCurrency = new SharkLocalCurrency(
                false,                // global limit
                currencyName.toString(),        // Name
                "A test Currency"               // Spec
        );

        // 2. Alice creates a new Group using the created Currency
        byte[] groupId = this.aliceCurrencyComponent.establishGroup(dummyCurrency,
                new ArrayList<>(),
                false,
                true);
        SharkGroupDocument testDoc = this.aliceStorage.getGroupDocument(groupId);
        byte[] aliceSignature = testDoc.getCurrentMembers().get(ALICE_ID);

        // 3. Checking results
        boolean verified = ASAPCryptoAlgorithms.verify(
                groupId, // Content which was signed
                aliceSignature,
                ALICE_ID,
                ((SharkPKIComponent) aliceSharkPeer
                        .getComponent(SharkPKIComponent.class))
                        .getASAPKeyStore());
        Assertions
                .assertEquals(ALICE_ID, testDoc.getGroupCreator());
        Assertions
                .assertTrue(verified, "The Signature of Alice could not have been verified");
        Assertions
                .assertEquals(1,testDoc.getCurrentMembers().size());
        Assertions
                .assertArrayEquals(aliceSignature, testDoc.getCurrentMembers().get(ALICE_ID),
                "The saved signature is different than the original one");
    }

    @Test
    public void establishGroupWithMemberThatIsNotWhitelisted()
            throws SharkException {

        // 0. Setting up Alice Peer
        this.setUpScenarioEstablishCurrency_1_justAlice();

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerB";
        SharkCurrency dummyCurrency = new SharkLocalCurrency(
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
                = assertThrows(SharkCurrencyException.class, () ->
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
        SharkCurrency dummyCurrency = new SharkLocalCurrency(
                false,
                currencyName.toString(),
                "A test Currency"
        );

        // 2. Alice creates a new Group and whitelists Bob
        ArrayList<CharSequence> whitelist = new ArrayList<>();
        whitelist.add(BOB_ID);

        byte[] groupId = this.aliceCurrencyComponent.establishGroup(
                dummyCurrency,
                whitelist,
                false,
                true);

        //Fehler behoben, dass es die uri nicht gefunden hat weil wir zu schnell waren
        Thread.sleep(2000);

        // 3. Encounter including message exchange starts, Alice will send a group invite to Bob the builder
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupId, "Hi Bob, join my group!", BOB_ID);

        // 4. Encounter
        this.runEncounter(this.aliceSharkPeer, this.bobSharkPeer, true);
        Thread.sleep(2000);

        // 5.(Assertions)
        SharkGroupDocument aliceDoc = this.aliceStorage.getGroupDocument(groupId);
        byte[] aliceSignature = aliceDoc.getCurrentMembers().get(ALICE_ID);
        boolean verifiedAliceSig = ASAPCryptoAlgorithms.verify(
                groupId,
                aliceSignature,
                ALICE_ID,
                ((SharkPKIComponent) aliceSharkPeer
                        .getComponent(SharkPKIComponent.class))
                        .getASAPKeyStore());
        
        Assertions
                .assertNotNull(aliceDoc, "Alice document is null.");
        //bob does not have the document stored -> Exception when asking for it
        Assertions
                .assertThrows(SharkCurrencyException.class, () -> {
            this.bobStorage.getGroupDocument(groupId);
        });

        Assertions
                .assertArrayEquals(groupId, this.aliceStorage.getGroupDocument(groupId).getGroupId());

        Assertions
                .assertEquals(1,this.bobStorage.getPendingInviteSize());

        //bob should have the invite pending
        Assertions
                .assertArrayEquals(groupId, this.bobStorage
                        .getPendingInvite(currencyName.toString()).getGroupId());
        //we expect 1 member. Just alice because bob didn't do anything yet
        Assertions
                .assertEquals(1,
                        aliceDoc.getCurrentMembers().size());
        //Alice signature has to be verified
        Assertions
                .assertTrue(verifiedAliceSig, "Alice signature is not verified");
    }


    @Test
    public void receiveInviteListenerAcceptedTest() throws SharkException, IOException, InterruptedException {
        // 0. Set up Alice and Bob
        this.setUpScenarioEstablishCurrency_2_BobAndAlice();

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerD";
        SharkCurrency dummyCurrency = new SharkLocalCurrency(
                false,
                currencyName.toString(),
                "A test Currency"
        );

        // 2. Alice creates a new Group and whitelists Bob
        ArrayList<CharSequence> whitelist = new ArrayList<>();
        whitelist.add(BOB_ID);

        byte[] groupId = this.aliceCurrencyComponent.establishGroup(
                dummyCurrency,
                whitelist,
                false,
                true);

        //Fehler behoben, dass es die uri nicht gefunden hat weil wir zu schnell waren
        Thread.sleep(2000);

        // 3. Encounter including message exchange starts, Alice will send a group invite to Bob the builder
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupId, "Hi Bob, join my group!", BOB_ID);

        // 4. Encounter
        this.runEncounter(this.aliceSharkPeer, this.bobSharkPeer, true);

        //5. Bob will accept the invitation
        this.bobImpl.acceptInviteAndSign(currencyName);

        Thread.sleep(2000);
        this.runEncounter(this.bobSharkPeer,this.aliceSharkPeer,true);
        Thread.sleep(2000);

        // 6.(Assertions)
        SharkGroupDocument aliceDoc = this.aliceStorage.getGroupDocument(groupId);
        SharkGroupDocument bobDoc = this.bobStorage.getGroupDocument(groupId);
        byte[] aliceSignature = bobDoc.getCurrentMembers().get(ALICE_ID);
        byte[] bobSignature = bobDoc.getCurrentMembers().get(BOB_ID);
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
        //both signatures are verified
        Assertions
                .assertTrue(verifiedAliceSig, "Alice signature is not verified");
        Assertions
                .assertTrue(verifiedBobSig, "Bob signature is not verified");
        //bob should have no pending invites, since he accepted
        Assertions.assertFalse(this.bobStorage.hasPendingInvites());
    }


    @Test
    public void receiveInviteListenerDeniedTest() throws SharkException, IOException, InterruptedException {

        // 0. Set up Alice and Bob
        this.setUpScenarioEstablishCurrency_2_BobAndAlice();

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerF";
        SharkCurrency dummyCurrency = new SharkLocalCurrency(
                false,
                currencyName.toString(),
                "A test Currency"
        );

        // 2. Alice creates a new Group and whitelists Bob
        ArrayList<CharSequence> whitelist = new ArrayList<>();
        whitelist.add(BOB_ID);

        byte[] groupId = this.aliceCurrencyComponent.establishGroup(
                dummyCurrency,
                whitelist,
                false,
                true);

        Thread.sleep(2000);

        // 3. Encounter including message exchange starts, Alice will send a group invite to Bob the builder
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupId, "Hi Bob, join my group!", BOB_ID);

        // 4. Encounter
        this.runEncounter(this.aliceSharkPeer, this.bobSharkPeer, true);

        //5. Bob will decline the invitation
        this.bobImpl.declineInvite(currencyName);

        //6. Assertions
        SharkGroupDocument aliceDoc = this.aliceStorage.getGroupDocument(groupId);

        Assertions.assertEquals(1,aliceDoc.getCurrentMembers().size());

        Assertions.assertFalse(this.bobStorage.hasPendingInvites());

        Assertions
                .assertThrows(SharkCurrencyException.class, () -> {
                    this.bobStorage.getGroupDocument(groupId);
                });

        // Überprüfen, dass Bob nicht Teil der Gruppe im lokalen Dokument von Alice ist
        Assertions.assertFalse(
                aliceDoc.getCurrentMembers().containsKey(BOB_ID),
                "Bob darf nicht in Alices GroupDocument auftauchen, da er der Gruppe nicht beigetreten ist."
        );

        // Der Status der Gruppe muss SIGNED_BY_SOME sein (da Bob auf der Whitelist steht, aber nicht signiert hat)
        Assertions.assertEquals(
                GroupSignings.SIGNED_BY_SOME,
                aliceDoc.getGroupDocState(),
                "Der DocState muss SIGNED_BY_SOME sein, da noch nicht alle Whitelist-Member beigetreten sind."
        );

    }


    @Test
    public void







    sendGroupInviteTo3PeersAndAllAccept() throws SharkException, IOException, InterruptedException {
        this.setUpScenarioEstablishCurrency_4_DavidAndClaraAndBobAndAlice();

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerG";
        SharkCurrency dummyCurrency = new SharkLocalCurrency(
                false,
                currencyName.toString(),
                "A test Currency"
        );

        // 2. Alice creates a new Group and whitelists Bob, Clara and David
        ArrayList<CharSequence> whitelist = new ArrayList<>();
        whitelist.add(BOB_ID);
        whitelist.add(CLARA_ID);
        whitelist.add(DAVID_ID);

        byte[] groupId = this.aliceCurrencyComponent.establishGroup(
                dummyCurrency,
                whitelist,
                false,
                true);

        // Zeit zum sicheren establishen der Gruppe
        Thread.sleep(2000);

        // 3. Encounter including message exchange starts, Alice will send a group invite to Bob, Clara and David the builder
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupId, "Hi Bob, join my group!", BOB_ID);
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupId, "Hi Clara, join my group!", CLARA_ID);
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupId, "Hi David, join my group!", DAVID_ID);

        // 4. Encounter
        this.runEncounter(this.aliceSharkPeer, this.bobSharkPeer, true);
        this.runEncounter(this.aliceSharkPeer, this.claraSharkPeer, true);
        this.runEncounter(this.aliceSharkPeer, this.davidSharkPeer, true);

        Thread.sleep(2000);

        // 5.1 Accept Invitation
        this.bobImpl.acceptInviteAndSign(currencyName);
        Thread.sleep(1000);
        this.runEncounter(this.bobSharkPeer, this.aliceSharkPeer, true);
        Thread.sleep(1000);
        this.claraImpl.acceptInviteAndSign(currencyName);
        Thread.sleep(1000);
        this.runEncounter(this.claraSharkPeer, this.aliceSharkPeer, true);
        Thread.sleep(1000);
        this.davidImpl.acceptInviteAndSign(currencyName);
        Thread.sleep(1000);
        this.runEncounter(this.davidSharkPeer, this.aliceSharkPeer, true);
        Thread.sleep(1000);
        //5.2 more encounters (we need better solution for this xd)
        this.runEncounter(this.bobSharkPeer, this.claraSharkPeer, true);
        this.runEncounter(this.bobSharkPeer, this.davidSharkPeer, true);
        this.runEncounter(this.claraSharkPeer, this.bobSharkPeer, true);
        this.runEncounter(this.claraSharkPeer, this.davidSharkPeer, true);
        this.runEncounter(this.davidSharkPeer, this.bobSharkPeer, true);
        this.runEncounter(this.davidSharkPeer, this.claraSharkPeer, true);
        Thread.sleep(2000);

        // 6.(Assertions)
        SharkGroupDocument aliceDoc = this.aliceStorage.getGroupDocument(groupId);
        SharkGroupDocument bobDoc = this.bobStorage.getGroupDocument(groupId);
        SharkGroupDocument claraDoc = this.claraStorage.getGroupDocument(groupId);
        SharkGroupDocument davidDoc = this.davidStorage.getGroupDocument(groupId);

        Assertions.assertNotNull(aliceDoc, "Alice document ist null.");
        Assertions.assertNotNull(bobDoc, "Bob document ist null.");
        Assertions.assertNotNull(claraDoc, "Clara document ist null.");
        Assertions.assertNotNull(davidDoc, "David document ist null.");

        Assertions.assertArrayEquals(groupId, bobDoc.getGroupId(), "Die GroupID bei Bob muss mit der von Alice übereinstimmen.");
        Assertions.assertArrayEquals(groupId, claraDoc.getGroupId(), "Die GroupID bei Clara muss mit der von Alice übereinstimmen.");
        Assertions.assertArrayEquals(groupId, davidDoc.getGroupId(), "Die GroupID bei David muss mit der von Alice übereinstimmen.");

        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, aliceDoc.getGroupDocState(), "Alice Group Document is not SIGNED_BY_ALL");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, bobDoc.getGroupDocState(), "Bobs Group Document is not SIGNED_BY_ALL");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, claraDoc.getGroupDocState(), "Claras Group Document is not SIGNED_BY_ALL");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, davidDoc.getGroupDocState(), "Davids Group Document is not SIGNED_BY_ALL");

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

        Assertions.assertFalse(this.bobStorage.hasPendingInvites(),
                "Bob should not have pending invited");
        Assertions.assertFalse(this.claraStorage.hasPendingInvites(),
                "Clara should not have pending invited");
        Assertions.assertFalse(this.davidStorage.hasPendingInvites(),
                "David should not have pending invited");
    }

    @Test
    public void sendGroupInviteTo3PeersAnd2AcceptAnd1Decline() throws SharkException, IOException, InterruptedException {
        this.setUpScenarioEstablishCurrency_4_DavidAndClaraAndBobAndAlice();

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerH";
        SharkCurrency dummyCurrency = new SharkLocalCurrency(
                false,
                currencyName.toString(),
                "A test Currency"
        );

        // 2. Alice creates a new Group and whitelists Bob, Clara and David
        ArrayList<CharSequence> whitelist = new ArrayList<>();
        whitelist.add(BOB_ID);
        whitelist.add(CLARA_ID);
        whitelist.add(DAVID_ID);

        byte[] groupId = this.aliceCurrencyComponent.establishGroup(
                dummyCurrency,
                whitelist,
                false,
                true);

        // Zeit zum sicheren establishen der Gruppe
        Thread.sleep(2000);

        // 3. Encounter including message exchange starts, Alice will send a group invite to Bob, Clara and David the builder
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupId, "Hi Bob, join my group!", BOB_ID);
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupId, "Hi Clara, join my group!", CLARA_ID);
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupId, "Hi David, join my group!", DAVID_ID);

        // 4. Encounter
        this.runEncounter(this.aliceSharkPeer, this.bobSharkPeer, true);
        this.runEncounter(this.aliceSharkPeer, this.claraSharkPeer, true);
        this.runEncounter(this.aliceSharkPeer, this.davidSharkPeer, true);

        Thread.sleep(2000);

        // 5. Accept and decline invitation
        // Bob and Clara accept
        this.bobImpl.acceptInviteAndSign(currencyName);
        this.claraImpl.acceptInviteAndSign(currencyName);
        // David declines
        this.davidImpl.declineInvite(currencyName);

        // Encounters
        Thread.sleep(1000);
        this.runEncounter(this.bobSharkPeer, this.aliceSharkPeer, true);
        Thread.sleep(1000);
        this.runEncounter(this.claraSharkPeer, this.aliceSharkPeer, true);
        Thread.sleep(1000);
        this.runEncounter(this.davidSharkPeer, this.aliceSharkPeer, true);

        this.runEncounter(this.bobSharkPeer, this.claraSharkPeer, true);
        this.runEncounter(this.bobSharkPeer, this.davidSharkPeer, true);
        this.runEncounter(this.claraSharkPeer, this.bobSharkPeer, true);
        this.runEncounter(this.claraSharkPeer, this.davidSharkPeer, true);
        this.runEncounter(this.davidSharkPeer, this.bobSharkPeer, true);
        this.runEncounter(this.davidSharkPeer, this.claraSharkPeer, true);
        Thread.sleep(2000);


        // 6.(Assertions)
        SharkGroupDocument aliceDoc = this.aliceStorage.getGroupDocument(groupId);
        SharkGroupDocument bobDoc = this.bobStorage.getGroupDocument(groupId);
        SharkGroupDocument claraDoc = this.claraStorage.getGroupDocument(groupId);

        Assertions.assertNotNull(aliceDoc, "Alice document ist null.");
        Assertions.assertNotNull(bobDoc, "Bob document ist null.");
        Assertions.assertNotNull(claraDoc, "Clara document ist null.");

        Assertions.assertEquals(3, aliceDoc.getCurrentMembers().size(), "Alice docs member count is not correct");
        Assertions.assertEquals(3, bobDoc.getCurrentMembers().size(), "Bobs docs member count is not correct");
        Assertions.assertEquals(3, claraDoc.getCurrentMembers().size(), "Claras docs member count is not correct");

        Assertions.assertArrayEquals(groupId, aliceDoc.getGroupId(), "Die GroupID bei Alice muss mit der von ihr erstellten Gruppe übereinstimmen.");
        Assertions.assertArrayEquals(groupId, bobDoc.getGroupId(), "Die GroupID bei Bob muss mit der von Alice übereinstimmen.");
        Assertions.assertArrayEquals(groupId, claraDoc.getGroupId(), "Die GroupID bei Clara muss mit der von Alice übereinstimmen.");

        // Group Document muss SIGNED_BY_SOME sein, da David in der Whitelist steht, allerdings die Gruppeneinladung abgelehnt hat
        Assertions.assertEquals(GroupSignings.SIGNED_BY_SOME, aliceDoc.getGroupDocState(), "Alice Group Document is not SIGNED_BY_SOME");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_SOME, bobDoc.getGroupDocState(), "Bobs Group Document is not SIGNED_BY_SOME");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_SOME, claraDoc.getGroupDocState(), "Claras Group Document is not SIGNED_BY_SOME");

        byte[] aliceSignature = aliceDoc.getCurrentMembers().get(ALICE_ID);
        byte[] bobSignature = bobDoc.getCurrentMembers().get(BOB_ID);
        byte[] claraSignature = claraDoc.getCurrentMembers().get(CLARA_ID);

        boolean verifiedAliceSig = ASAPCryptoAlgorithms.verify(groupId, aliceSignature, ALICE_ID,
                ((SharkPKIComponent) aliceSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        boolean verifiedBobSig = ASAPCryptoAlgorithms.verify(groupId, bobSignature, BOB_ID,
                ((SharkPKIComponent) bobSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        boolean verifiedClaraSig = ASAPCryptoAlgorithms.verify(groupId, claraSignature, CLARA_ID,
                ((SharkPKIComponent) claraSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());

        Assertions.assertTrue(verifiedAliceSig, "Alice Signatur ist ungültig");
        Assertions.assertTrue(verifiedBobSig, "Bob Signatur ist ungültig");
        Assertions.assertTrue(verifiedClaraSig, "Clara Signatur ist ungültig");

        Assertions.assertFalse(this.bobStorage.hasPendingInvites(),
                "Bob should not have pending invited");
        Assertions.assertFalse(this.claraStorage.hasPendingInvites(),
                "Clara should not have pending invited");
        Assertions.assertFalse(this.davidStorage.hasPendingInvites(),
                "David should not have pending invited");
    }

    @Test
    public void groupWith2WhitlistedButInvite3() throws SharkException, InterruptedException, IOException {
        this.setUpScenarioEstablishCurrency_4_DavidAndClaraAndBobAndAlice();

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerI";
        SharkCurrency dummyCurrency = new SharkLocalCurrency(
                false,
                currencyName.toString(),
                "A test Currency"
        );

        // 2. Alice creates a new Group and whitelists Bob and Clara, without David
        ArrayList<CharSequence> whitelist = new ArrayList<>();
        whitelist.add(BOB_ID);
        whitelist.add(CLARA_ID);

        // TODO: Encrypten der Einladungen für Whitelisted Gruppen immer, oder? Muss noch gemacht werden, da David das GroupDoc lesen kann!
        byte[] groupId = this.aliceCurrencyComponent.establishGroup(
                dummyCurrency,
                whitelist,
                true,
                true);

        Thread.sleep(1000);

        // 3. Encounter including message exchange starts, Alice will send a group invite to Bob, Clara and David the builder
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupId, "Hi Bob, join my group!", BOB_ID);
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupId, "Hi Clara, join my group!", CLARA_ID);
        String errorMessage = "";
        try {
            this.aliceCurrencyComponent
                    .invitePeerToGroup(groupId, "Hi David, join my group!", DAVID_ID);
        } catch (SharkCurrencyException e) {
            errorMessage=e.getMessage();
        }

        // 4. Encounter
        this.runEncounter(this.aliceSharkPeer, this.bobSharkPeer, true);
        this.runEncounter(this.aliceSharkPeer, this.claraSharkPeer, true);
        this.runEncounter(this.aliceSharkPeer, this.davidSharkPeer, true);

        Thread.sleep(2000);

        // 5. Accept and decline invitation
        // Bob, Clara and David try to accept the invitation
        this.bobImpl.acceptInviteAndSign(currencyName);
        this.claraImpl.acceptInviteAndSign(currencyName);

        // Encounters
        Thread.sleep(1000);
        this.runEncounter(this.bobSharkPeer, this.aliceSharkPeer, true);
        Thread.sleep(1000);
        this.runEncounter(this.claraSharkPeer, this.aliceSharkPeer, true);

        this.runEncounter(this.bobSharkPeer, this.claraSharkPeer, true);
        this.runEncounter(this.claraSharkPeer, this.bobSharkPeer, true);
        this.runEncounter(this.davidSharkPeer, this.bobSharkPeer, true);
        Thread.sleep(2000);

        // 6.(Assertions)
        SharkGroupDocument aliceDoc = this.aliceStorage.getGroupDocument(groupId);
        SharkGroupDocument bobDoc = this.bobStorage.getGroupDocument(groupId);
        SharkGroupDocument claraDoc = this.claraStorage.getGroupDocument(groupId);

        Assertions.assertNotNull(aliceDoc, "Alice document ist null.");
        Assertions.assertNotNull(bobDoc, "Bob document ist null.");
        Assertions.assertNotNull(claraDoc, "Clara document ist null.");

        String exceptedMessage = "Peer with id: " + DAVID_ID + " can not be invited because this peer is not whitelisted.";
        Assertions.assertEquals(errorMessage, exceptedMessage);

        // David should not be member in any document
        Assertions.assertFalse(aliceDoc.getCurrentMembers().containsKey(DAVID_ID), "David should not be member in aliceDoc.");
        Assertions.assertFalse(bobDoc.getCurrentMembers().containsKey(DAVID_ID), "David should not be member in bobDoc.");
        Assertions.assertFalse(claraDoc.getCurrentMembers().containsKey(DAVID_ID), "David should not be member in claraDoc.");

        Assertions.assertEquals(3, aliceDoc.getCurrentMembers().size(), "Alice docs member count is not correct");
        Assertions.assertEquals(3, bobDoc.getCurrentMembers().size(), "Bobs docs member count is not correct");
        Assertions.assertEquals(3, claraDoc.getCurrentMembers().size(), "Claras docs member count is not correct");

        Assertions.assertArrayEquals(groupId, aliceDoc.getGroupId());
        Assertions.assertArrayEquals(groupId, bobDoc.getGroupId());
        Assertions.assertArrayEquals(groupId, claraDoc.getGroupId());

        // Group Document is SIGNED_BY_All
        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, aliceDoc.getGroupDocState(), "Alice Group Document is not SIGNED_BY_SOME");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, bobDoc.getGroupDocState(), "Bobs Group Document is not SIGNED_BY_SOME");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, claraDoc.getGroupDocState(), "Claras Group Document is not SIGNED_BY_SOME");

        byte[] aliceSignature = aliceDoc.getCurrentMembers().get(ALICE_ID);
        byte[] bobSignature = bobDoc.getCurrentMembers().get(BOB_ID);
        byte[] claraSignature = claraDoc.getCurrentMembers().get(CLARA_ID);

        boolean verifiedAliceSig = ASAPCryptoAlgorithms.verify(groupId, aliceSignature, ALICE_ID,
                ((SharkPKIComponent) aliceSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        boolean verifiedBobSig = ASAPCryptoAlgorithms.verify(groupId, bobSignature, BOB_ID,
                ((SharkPKIComponent) bobSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        boolean verifiedClaraSig = ASAPCryptoAlgorithms.verify(groupId, claraSignature, CLARA_ID,
                ((SharkPKIComponent) claraSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());

        Assertions.assertTrue(verifiedAliceSig, "Alice Signatur ist ungültig");
        Assertions.assertTrue(verifiedBobSig, "Bob Signatur ist ungültig");
        Assertions.assertTrue(verifiedClaraSig, "Clara Signatur ist ungültig");

        Assertions.assertFalse(this.aliceStorage.hasPendingInvites(),
                "Alice should not have pending invites");
        Assertions.assertFalse(this.bobStorage.hasPendingInvites(),
                "Bob should not have pending invites");
        Assertions.assertFalse(this.claraStorage.hasPendingInvites(),
                "Clara should not have pending invites");
        Assertions.assertFalse(this.davidStorage.hasPendingInvites(),
                "David should not have pending invites");
    }


    @Test
    public void sendEncryptedInvitation(){

    }

    @Test
    public void sendInvitationFor2GroupsFromSamePeer() throws SharkException, InterruptedException, IOException {
        this.setUpScenarioEstablishCurrency_4_DavidAndClaraAndBobAndAlice();

        // 1. Alice arranges two new local Currencies
        CharSequence currencyNameA = "AliceTaler1A";
        SharkCurrency dummyCurrencyA = new SharkLocalCurrency(
                false,
                currencyNameA.toString(),
                "A test Currency"
        );

        CharSequence currencyNameB = "AliceTaler1B";
        SharkCurrency dummyCurrencyB = new SharkLocalCurrency(
                false,
                currencyNameB.toString(),
                "A test Currency"
        );

        // 2. Alice creates two Groups and whitelists Bob, Clara and David for both of them
        ArrayList<CharSequence> whitelist = new ArrayList<>();
        whitelist.add(BOB_ID);
        whitelist.add(CLARA_ID);
        whitelist.add(DAVID_ID);

        byte[] groupIdA = this.aliceCurrencyComponent.establishGroup(
                dummyCurrencyA,
                whitelist,
                false,
                true);

        byte[] groupIdB = this.aliceCurrencyComponent.establishGroup(
                dummyCurrencyB,
                whitelist,
                false,
                true);

        // Zeit zum sicheren establishen der Gruppe
        Thread.sleep(2000);

        // 3. Encounter including message exchange starts, Alice will send a group invite to Bob, Clara and David
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupIdA, "Hi Bob, join my group!", BOB_ID);
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupIdA, "Hi Clara, join my group!", CLARA_ID);
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupIdA, "Hi David, join my group!", DAVID_ID);
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupIdB, "Hi Bob, join my group!", BOB_ID);
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupIdB, "Hi Clara, join my group!", CLARA_ID);
        this.aliceCurrencyComponent
                .invitePeerToGroup(groupIdB, "Hi David, join my group!", DAVID_ID);

        // 4. Encounter
        this.runEncounter(this.aliceSharkPeer, this.bobSharkPeer, true);
        this.runEncounter(this.aliceSharkPeer, this.claraSharkPeer, true);
        this.runEncounter(this.aliceSharkPeer, this.davidSharkPeer, true);

        Thread.sleep(2000);

        // 5.1 Accept Invitation
        this.bobImpl.acceptInviteAndSign(currencyNameA);
        this.bobImpl.acceptInviteAndSign(currencyNameB);
        Thread.sleep(1000);
        this.runEncounter(this.bobSharkPeer, this.aliceSharkPeer, true);
        Thread.sleep(1000);
        this.claraImpl.acceptInviteAndSign(currencyNameA);
        this.claraImpl.acceptInviteAndSign(currencyNameB);
        Thread.sleep(1000);
        this.runEncounter(this.claraSharkPeer, this.aliceSharkPeer, true);
        Thread.sleep(1000);
        this.davidImpl.acceptInviteAndSign(currencyNameA);
        this.davidImpl.acceptInviteAndSign(currencyNameB);
        Thread.sleep(1000);
        this.runEncounter(this.davidSharkPeer, this.aliceSharkPeer, true);
        Thread.sleep(1000);
        //5.2 more encounters (we need better solution for this xd)
        this.runEncounter(this.bobSharkPeer, this.claraSharkPeer, true);
        this.runEncounter(this.bobSharkPeer, this.davidSharkPeer, true);
        this.runEncounter(this.claraSharkPeer, this.bobSharkPeer, true);
        this.runEncounter(this.claraSharkPeer, this.davidSharkPeer, true);
        this.runEncounter(this.davidSharkPeer, this.bobSharkPeer, true);
        this.runEncounter(this.davidSharkPeer, this.claraSharkPeer, true);
        Thread.sleep(2000);

        // 6.(Assertions)
        SharkGroupDocument aliceDocA = this.aliceStorage.getGroupDocument(groupIdA);
        SharkGroupDocument bobDocA = this.bobStorage.getGroupDocument(groupIdA);
        SharkGroupDocument claraDocA = this.claraStorage.getGroupDocument(groupIdA);
        SharkGroupDocument davidDocA = this.davidStorage.getGroupDocument(groupIdA);

        SharkGroupDocument aliceDocB = this.aliceStorage.getGroupDocument(groupIdB);
        SharkGroupDocument bobDocB = this.bobStorage.getGroupDocument(groupIdB);
        SharkGroupDocument claraDocB = this.claraStorage.getGroupDocument(groupIdB);
        SharkGroupDocument davidDocB = this.davidStorage.getGroupDocument(groupIdB);

        Assertions.assertNotNull(aliceDocA, "Alice document A ist null.");
        Assertions.assertNotNull(bobDocA, "Bob document A ist null.");
        Assertions.assertNotNull(claraDocA, "Clara document A ist null.");
        Assertions.assertNotNull(davidDocA, "David document A ist null.");

        Assertions.assertNotNull(aliceDocB, "Alice document B ist null.");
        Assertions.assertNotNull(bobDocB, "Bob document B ist null.");
        Assertions.assertNotNull(claraDocB, "Clara document B ist null.");
        Assertions.assertNotNull(davidDocB, "David document B ist null.");

        Assertions.assertArrayEquals(groupIdA, bobDocA.getGroupId(), "Die GroupID_A bei Bob muss mit der von Alice übereinstimmen.");
        Assertions.assertArrayEquals(groupIdA, claraDocA.getGroupId(), "Die GroupID_A bei Clara muss mit der von Alice übereinstimmen.");
        Assertions.assertArrayEquals(groupIdA, davidDocA.getGroupId(), "Die GroupID_A bei David muss mit der von Alice übereinstimmen.");

        Assertions.assertArrayEquals(groupIdB, bobDocB.getGroupId(), "Die GroupID_B bei Bob muss mit der von Alice übereinstimmen.");
        Assertions.assertArrayEquals(groupIdB, claraDocB.getGroupId(), "Die GroupID_B bei Clara muss mit der von Alice übereinstimmen.");
        Assertions.assertArrayEquals(groupIdB, davidDocB.getGroupId(), "Die GroupID_B bei David muss mit der von Alice übereinstimmen.");

        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, aliceDocA.getGroupDocState(), "Alice Group Document A is not SIGNED_BY_ALL");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, bobDocA.getGroupDocState(), "Bobs Group Document A is not SIGNED_BY_ALL");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, claraDocA.getGroupDocState(), "Claras Group Document A is not SIGNED_BY_ALL");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, davidDocA.getGroupDocState(), "Davids Group Document A is not SIGNED_BY_ALL");

        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, aliceDocB.getGroupDocState(), "Alice Group Document B is not SIGNED_BY_ALL");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, bobDocB.getGroupDocState(), "Bobs Group Document B is not SIGNED_BY_ALL");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, claraDocB.getGroupDocState(), "Claras Group Document B is not SIGNED_BY_ALL");
        Assertions.assertEquals(GroupSignings.SIGNED_BY_ALL, davidDocB.getGroupDocState(), "Davids Group Document B is not SIGNED_BY_ALL");

        Assertions
                .assertEquals(4,
                        aliceDocA.getCurrentMembers().size(),
                        "Alice docsA member count is not correct");
        Assertions
                .assertEquals(4,
                        bobDocA.getCurrentMembers().size(),
                        "Bob docsA member count is not correct");
        Assertions
                .assertEquals(4,
                        claraDocA.getCurrentMembers().size(),
                        "Clara docsA member count is not correct");
        Assertions
                .assertEquals(4,
                        davidDocA.getCurrentMembers().size(),
                        "David docsA member count is not correct");

        Assertions
                .assertEquals(4,
                        aliceDocB.getCurrentMembers().size(),
                        "Alice docsB member count is not correct");
        Assertions
                .assertEquals(4,
                        bobDocB.getCurrentMembers().size(),
                        "Bob docsB member count is not correct");
        Assertions
                .assertEquals(4,
                        claraDocB.getCurrentMembers().size(),
                        "Clara docsB member count is not correct");
        Assertions
                .assertEquals(4,
                        davidDocB.getCurrentMembers().size(),
                        "David docsB member count is not correct");

        byte[] aliceSignatureA = aliceDocA.getCurrentMembers().get(ALICE_ID);
        byte[] bobSignatureA = bobDocA.getCurrentMembers().get(BOB_ID);
        byte[] claraSignatureA = claraDocA.getCurrentMembers().get(CLARA_ID);
        byte[] davidSignatureA = davidDocA.getCurrentMembers().get(DAVID_ID);

        byte[] aliceSignatureB = aliceDocB.getCurrentMembers().get(ALICE_ID);
        byte[] bobSignatureB = bobDocB.getCurrentMembers().get(BOB_ID);
        byte[] claraSignatureB = claraDocB.getCurrentMembers().get(CLARA_ID);
        byte[] davidSignatureB = davidDocB.getCurrentMembers().get(DAVID_ID);

        boolean verifiedAliceSigA = ASAPCryptoAlgorithms.verify(groupIdA, aliceSignatureA, ALICE_ID,
                ((SharkPKIComponent) aliceSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        boolean verifiedBobSigA = ASAPCryptoAlgorithms.verify(groupIdA, bobSignatureA, BOB_ID,
                ((SharkPKIComponent) bobSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        boolean verifiedClaraSigA = ASAPCryptoAlgorithms.verify(groupIdA, claraSignatureA, CLARA_ID,
                ((SharkPKIComponent) claraSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        boolean verifiedDavidSigA = ASAPCryptoAlgorithms.verify(groupIdA, davidSignatureA, DAVID_ID,
                ((SharkPKIComponent) davidSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());

        boolean verifiedAliceSigB = ASAPCryptoAlgorithms.verify(groupIdB, aliceSignatureB, ALICE_ID,
                ((SharkPKIComponent) aliceSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        boolean verifiedBobSigB = ASAPCryptoAlgorithms.verify(groupIdB, bobSignatureB, BOB_ID,
                ((SharkPKIComponent) bobSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        boolean verifiedClaraSigB = ASAPCryptoAlgorithms.verify(groupIdB, claraSignatureB, CLARA_ID,
                ((SharkPKIComponent) claraSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        boolean verifiedDavidSigB = ASAPCryptoAlgorithms.verify(groupIdB, davidSignatureB, DAVID_ID,
                ((SharkPKIComponent) davidSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());

        Assertions.assertTrue(verifiedAliceSigA, "Alice Signatur A ist ungültig");
        Assertions.assertTrue(verifiedBobSigA, "Bob Signatur A ist ungültig");
        Assertions.assertTrue(verifiedClaraSigA, "Clara Signatur A ist ungültig");
        Assertions.assertTrue(verifiedDavidSigA, "David Signatur A ist ungültig");

        Assertions.assertTrue(verifiedAliceSigB, "Alice Signatur B ist ungültig");
        Assertions.assertTrue(verifiedBobSigB, "Bob Signatur B ist ungültig");
        Assertions.assertTrue(verifiedClaraSigB, "Clara Signatur B ist ungültig");
        Assertions.assertTrue(verifiedDavidSigB, "David Signatur B ist ungültig");

        Assertions.assertFalse(this.bobStorage.hasPendingInvites(),
                "Bob should not have pending invited");
        Assertions.assertFalse(this.claraStorage.hasPendingInvites(),
                "Clara should not have pending invited");
        Assertions.assertFalse(this.davidStorage.hasPendingInvites(),
                "David should not have pending invited");
    }

    @Test
    public void sendInvitationFor2GroupsFromDifferentPeers(){

    }
}