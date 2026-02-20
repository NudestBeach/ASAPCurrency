package establishCurrencyTests;
import Group.SharkGroupDocument;
import currency.api.SharkCurrencyComponent;
import org.apache.commons.io.FileUtils;
import currency.classes.Currency;
import currency.classes.LocalCurrency;
import exepections.ASAPCurrencyException;

import net.sharksystem.asap.*;
import net.sharksystem.utils.streams.StreamPairImpl;
import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPEncounterManagerImpl;
import net.sharksystem.asap.apps.TCPServerSocketAcceptor;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;

import net.sharksystem.pki.SharkPKIComponent;

import net.sharksystem.utils.streams.StreamPairImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testHelper.AsapCurrencyTestHelper;


import java.io.File;
import java.io.IOException;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class EstablishCurrencyTests extends AsapCurrencyTestHelper {

    public EstablishCurrencyTests() {
        super(EstablishCurrencyTests.class.getSimpleName());
    }


    @AfterEach
    void tearDown() throws Exception {

        if (this.aliceSharkPeer != null) {
            this.aliceSharkPeer.stop();
        }
        if (this.bobSharkPeer != null) {
            this.bobSharkPeer.stop();
        }
        File testFolder = new File("testResultsRootFolder");
        if (testFolder.exists()) {
            try {
                FileUtils.forceDelete(testFolder);
                System.out.println("Deleted: " + testFolder.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Konnte nicht löschen - File noch gelockt: " + e.getMessage());
            }
        }
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
                new ArrayList<CharSequence>(),
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
                = assertThrows(ASAPCurrencyException.class, () -> {
            this.aliceCurrencyComponent.establishGroup(membersToBeInvited,
                    dummyCurrency,
                    new ArrayList<CharSequence>(),
                    false,
                    true);
        });
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


        // 5.(Assertions)
        SharkGroupDocument aliceDoc = this.aliceImpl.getSharkGroupDocument(currencyName);
        byte[] groupId = aliceDoc.getGroupId();
        SharkGroupDocument bobDoc = this.bobImpl.getSharkGroupDocument(currencyName);
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
                .assertNotNull(bobDoc, "Bob document ist null.");
        Assertions
                .assertArrayEquals(groupId,
                        bobDoc.getGroupId(),
                        "Die GroupID bei Bob muss mit der von Alice übereinstimmen.");
        Assertions
                .assertEquals(3,
                        bobDoc.getCurrentMembers().size()
                                + aliceDoc.getCurrentMembers().size()); //we expect 2 members each, alice and bob
        //Alice and Bob have to be verified
        Assertions
                .assertTrue(verifiedAliceSig, "Alice Signatur ist ungültig");
        Assertions
                .assertTrue(verifiedAliceSig, "Bob Signatur ist ungültig");

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

        this.bobImpl.acceptInvite(this.bobImpl.getSharkGroupDocument(currencyName));

        // 6.(Assertions)
        SharkGroupDocument aliceDoc = this.aliceImpl.getSharkGroupDocument(currencyName);
        byte[] groupId = aliceDoc.getGroupId();
        SharkGroupDocument bobDoc = this.bobImpl.getSharkGroupDocument(currencyName);
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
                .assertNotNull(bobDoc, "Bob document ist null.");
        Assertions
                .assertArrayEquals(groupId,
                        bobDoc.getGroupId(),
                        "Die GroupID bei Bob muss mit der von Alice übereinstimmen.");
        Assertions
                .assertEquals(3,
                        bobDoc.getCurrentMembers().size()
                                + aliceDoc.getCurrentMembers().size()); //we expect 2 members each, alice and bob
        //Alice and Bob have to be verified
        Assertions
                .assertTrue(verifiedAliceSig, "Alice Signatur ist ungültig");
        Assertions
                .assertTrue(verifiedAliceSig, "Bob Signatur ist ungültig");


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

        //5. Bob will accept the invitation

        this.bobImpl.declineInvite(this.bobImpl.getSharkGroupDocument(currencyName));


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

        // 5. Accept Invitation
        this.bobImpl.acceptInvite(this.bobImpl.getSharkGroupDocument(currencyName));
        this.claraImpl.acceptInvite(this.claraImpl.getSharkGroupDocument(currencyName));
        this.davidImpl.acceptInvite(this.davidImpl.getSharkGroupDocument(currencyName));

        // 6.(Assertions)
        SharkGroupDocument aliceDoc = this.aliceImpl.getSharkGroupDocument(currencyName);
        SharkGroupDocument bobDoc = this.bobImpl.getSharkGroupDocument(currencyName);
        SharkGroupDocument claraDoc = this.claraImpl.getSharkGroupDocument(currencyName);
        SharkGroupDocument davidDoc = this.davidImpl.getSharkGroupDocument(currencyName);

        byte[] groupId = aliceDoc.getGroupId();

        Assertions.assertNotNull(bobDoc, "Bob document ist null.");
        Assertions.assertNotNull(claraDoc, "Clara document ist null.");
        Assertions.assertNotNull(davidDoc, "David document ist null.");

        Assertions.assertArrayEquals(groupId, bobDoc.getGroupId(), "Die GroupID bei Bob muss mit der von Alice übereinstimmen.");
        Assertions.assertArrayEquals(groupId, claraDoc.getGroupId(), "Die GroupID bei Clara muss mit der von Alice übereinstimmen.");
        Assertions.assertArrayEquals(groupId, davidDoc.getGroupId(), "Die GroupID bei David muss mit der von Alice übereinstimmen.");

        // Alice hat sich selbst und alle anderen Alice + sich selbst (2)
        Assertions.assertEquals(7,
                aliceDoc.getCurrentMembers().size() +
                        bobDoc.getCurrentMembers().size() +
                        claraDoc.getCurrentMembers().size() +
                        davidDoc.getCurrentMembers().size(),
                "Die Summe der Mitglieder in den lokalen Dokumenten stimmt nicht.");

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