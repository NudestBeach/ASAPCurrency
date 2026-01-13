package establishCurrencyTests;
import Group.SharkGroupDocument;
import currency.api.SharkCurrencyComponent;
import currency.api.SharkCurrencyComponentFactory;
import currency.classes.Currency;
import currency.classes.LocalCurrency;
import exepections.ASAPCurrencyException;
import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.asap.crypto.ASAPKeyStore;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.testhelper.SharkPKITesthelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import testHelper.AsapCurrencyTestHelper;
import testhelpers.SharkCurrencyComponentImpl;

import java.io.IOException;
import java.util.*;

import static net.sharksystem.utils.testsupport.TestConstants.*;
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
                new ArrayList<>(),                // centralized list (dummy)
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
                groupId,
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
                new ArrayList<>(),                // centralized list (dummy)
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
        setUpScenarioEstablishCurrency_2_BobAndAlice();

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerC";
        Currency dummyCurrency = new LocalCurrency(
                false,
                new ArrayList<>(),
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

        SharkGroupDocument testDoc = this.aliceImpl.getSharkGroupDocument(currencyName);
        byte[] groupId = testDoc.getGroupId();

        bobSharkPeer.getASAPTestPeerFS()
                .getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT)
                .createChannel(currencyName);
        int port = 7777;

        Thread bobThread = new Thread(() -> {
            try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(port)) {

                java.net.Socket clientSocket = serverSocket.accept();



                bobSharkPeer.getASAPTestPeerFS().handleConnection(
                        clientSocket.getInputStream(),
                        clientSocket.getOutputStream()
                );
            } catch (IOException | SharkException e) {
                e.printStackTrace();
            }
        });
        bobThread.start();

        Thread.sleep(200);


        Thread aliceThread = new Thread(() -> {
            try (java.net.Socket socket = new java.net.Socket("localhost", port)) {


                aliceSharkPeer.getASAPTestPeerFS().handleConnection(
                        socket.getInputStream(),
                        socket.getOutputStream()
                );
            } catch (IOException | SharkException e) {
                e.printStackTrace();
            }
        });
        aliceThread.start();

        aliceThread.join(5000);
        bobThread.join(5000);

        // 4.(Assertions)
        SharkGroupDocument aliceDoc = this.aliceImpl.getSharkGroupDocument(currencyName);
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
                .assertNotNull(bobDoc, "Bob sollte das SharkGroupDocument empfangen haben.");
        Assertions
                .assertArrayEquals(groupId,
                        bobDoc.getGroupId(),
                        "Die GroupID bei Bob muss mit der von Alice übereinstimmen.");
        Assertions
                .assertEquals(4,
                        bobDoc.getCurrentMembers().size()
                        +aliceDoc.getCurrentMembers().size()); //we expect 2 members each, alice and bob
        Assertions
                .assertTrue(verifiedAliceSig&&verifiedBobSig); //both have to be verified
    }
}