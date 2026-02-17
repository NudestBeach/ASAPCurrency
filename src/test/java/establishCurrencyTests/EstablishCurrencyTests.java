package establishCurrencyTests;
import Group.SharkGroupDocument;
import currency.api.SharkCurrencyComponent;

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
import org.junit.jupiter.api.Test;
import testHelper.AsapCurrencyTestHelper;


import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class EstablishCurrencyTests extends AsapCurrencyTestHelper {

    public EstablishCurrencyTests() {
        super(EstablishCurrencyTests.class.getSimpleName());
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
        CharSequence groupUriA = SharkGroupDocument.DOCUMENT_FORMAT+currencyName;

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
        System.out.println("DEBUG: all channels: "+aliceStorage.getChannelURIs());
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

        AsapCurrencyTestHelper asapCurrencyTestHelper = new AsapCurrencyTestHelper("Successful group Invite Test");
        asapCurrencyTestHelper.setUpScenarioEstablishCurrency_2_BobAndAlice();

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

        asapCurrencyTestHelper.aliceCurrencyComponent.establishGroup(
                dummyCurrency,
                whitelist,
                false,
                true);
        //ALLICE to be online and visible
        ASAPPeerFS aliceASAPPeerFS = asapCurrencyTestHelper.aliceSharkPeer.getASAPTestPeerFS();
        ASAPEncounterManagerImpl aliceEncounterManager =
                new ASAPEncounterManagerImpl(aliceASAPPeerFS, aliceASAPPeerFS.getPeerID());

        //BOB to be online and visible
        ASAPPeerFS bobASAPPeerFS = asapCurrencyTestHelper.bobSharkPeer.getASAPTestPeerFS();
        ASAPEncounterManagerImpl bobEncounterManager =
                new ASAPEncounterManagerImpl(bobASAPPeerFS, bobASAPPeerFS.getPeerID());

        int alicePort = AsapCurrencyTestHelper.getPortNumber();
        TCPServerSocketAcceptor aliceSocketAcceptor = new TCPServerSocketAcceptor(alicePort, aliceEncounterManager);
        try {
            Socket connect2Alice = new Socket("localhost", alicePort);

            bobEncounterManager.handleEncounter(
                    StreamPairImpl.getStreamPair(
                            connect2Alice.getInputStream(), connect2Alice.getOutputStream(), net.sharksystem.utils.testsupport.TestConstants.ALICE_ID, net.sharksystem.utils.testsupport.TestConstants.ALICE_ID),
                    ASAPEncounterConnectionType.INTERNET);
        //Fehler behoben dass es die uri nicht gefunden hat weil wir zu schnell waren
        Thread.sleep(5000);
            // 3. Encounter including message exchange starts, Alice will send a group invite to Bob the builder
            asapCurrencyTestHelper.aliceCurrencyComponent
                    .invitePeerToGroup(currencyName,"Hi Bob, join my group!", BOB_ID);
            Thread.sleep(5000);
            asapCurrencyTestHelper.runEncounter(asapCurrencyTestHelper.aliceSharkPeer, asapCurrencyTestHelper.bobSharkPeer, true);

            aliceSocketAcceptor.close();

            // 4.(Assertions)
            SharkGroupDocument aliceDoc = asapCurrencyTestHelper.aliceImpl.getSharkGroupDocument(currencyName);
            byte[] groupId = aliceDoc.getGroupId();
            SharkGroupDocument bobDoc = asapCurrencyTestHelper.bobImpl.getSharkGroupDocument(currencyName);
            byte[] aliceSignature = bobDoc.getCurrentMembers().get(ALICE_ID);
            byte[] bobSignature = bobDoc.getCurrentMembers().get(BOB_ID);
            boolean verifiedAliceSig = ASAPCryptoAlgorithms.verify(
                    groupId,
                    aliceSignature,
                    ALICE_ID,
                    ((SharkPKIComponent) asapCurrencyTestHelper.aliceSharkPeer
                            .getComponent(SharkPKIComponent.class))
                            .getASAPKeyStore());
            boolean verifiedBobSig = ASAPCryptoAlgorithms.verify(
                    groupId,
                    bobSignature,
                    BOB_ID,
                    ((SharkPKIComponent) asapCurrencyTestHelper.bobSharkPeer
                            .getComponent(SharkPKIComponent.class))
                            .getASAPKeyStore());

            Assertions
                    .assertNotNull(bobDoc, "Bob document ist null.");
            Assertions
                    .assertArrayEquals(groupId,
                            bobDoc.getGroupId(),
                            "Die GroupID bei Bob muss mit der von Alice übereinstimmen.");
            Assertions
                    .assertEquals(4,
                            bobDoc.getCurrentMembers().size()
                                    +aliceDoc.getCurrentMembers().size()); //we expect 2 members each, alice and bob
            //Alice and Bob have to be verified
            Assertions
                    .assertTrue(verifiedAliceSig, "Alice Signatur ist ungültig");
            Assertions
                    .assertTrue(verifiedBobSig, "Bob Signatur ist ungültig");
        } finally {
            aliceSocketAcceptor.close();
        }

    }

    @Test
    public void sendGroupInviteTomorethanOnePerson(){

    }

    @Test
    public void receiveInviteListenerTest() throws SharkException {
        // 0. Setup Alice & Bob
        this.setUpScenarioEstablishCurrency_2_BobAndAlice();

        // 1. Währung vorbereiten
        CharSequence currencyName = "AliceTalerD";
        Currency dummyCurrency = new LocalCurrency(
                false,
                currencyName.toString(),
                "A test Currency"
        );

        // 2. Listener bei Bob registrieren
        // Wir nutzen ein Future, um das Ergebnis des Listeners einzufangen
        CompletableFuture<SharkGroupDocument> receivedGroupDocFuture = new CompletableFuture<>();

        // Bob lauscht nach einem Group Invite
        // TODO: Group Invite Listener hinzufügen
        /**
        this.bobCurrencyComponent.addGroupInviteListener(new ASAPGroupInviteListener() {
            @Override
            public void onGroupInviteReceived(String currencyID, SharkGroupDocument groupDocument){
                System.out.println("TEST-GROUP-INVITE-RECEIVED-LISTENER: Bob hat eine Einlaudung erhalten für " + currencyID);
                receivedGroupDocFuture(groupDocument);
            }
        });
         **/

        // 3. Alice erstellt Gruppe (und löst Gossip aus)
        ArrayList<CharSequence> whitelist = new ArrayList<>();
        whitelist.add(BOB_ID);

        this.aliceCurrencyComponent.establishGroup(
                dummyCurrency,
                whitelist,
                false,
                true);
        // 4. Netzwerk simulieren

    }
}