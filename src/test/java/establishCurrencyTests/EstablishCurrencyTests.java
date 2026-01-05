package establishCurrencyTests;
import Group.SharkGroupDocument;
import currency.api.SharkCurrencyComponent;
import currency.api.SharkCurrencyComponentFactory;
import currency.classes.Currency;
import currency.classes.LocalCurrency;
import exepections.ASAPCurrencyException;
import net.sharksystem.SharkException;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
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

        // 2. Alice creates a new Group using the created Currency
        this.aliceCurrencyComponent.establishGroup(dummyCurrency,
                new ArrayList<CharSequence>(),
                false,
                true);
        SharkGroupDocument testDoc = this.aliceImpl.getSharkGroupDocument(currencyName);
        byte[] groupId = testDoc.getGroupId();
        byte[] aliceSignature = testDoc.getCurrentMembers().get(ALICE_ID);

        // 3. Checking results
        Map<CharSequence, byte[]> members = testDoc.getCurrentMembers();
        boolean channelExists = aliceSharkPeer.getASAPPeer()
                .getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT)
                .channelExists(currencyName);
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
    }

    @Test
    public void establishGroupWithMemberThatIsNotWhitelisted()
            throws SharkException {

        // 0. Setting up Alice Peer
        SharkPKITesthelper.incrementTestNumber();
        String folderName = SharkPKITesthelper.getPKITestFolder(ROOT_DIRECTORY);
        aliceSharkPeer = SharkPKITesthelper.setupSharkPeerDoNotStart(ALICE_NAME, folderName);
        SharkPKIComponent pkiForFactory
                = SharkPKITesthelper.setupPKIComponentPeerNotStarted(aliceSharkPeer, ALICE_ID);
        SharkCurrencyComponentFactory currencyFactory
                = new SharkCurrencyComponentFactory(pkiForFactory);
        aliceSharkPeer.addComponent(currencyFactory, SharkCurrencyComponent.class);
        aliceSharkPeer.start(ALICE_ID);

        SharkCurrencyComponentImpl currencyComponent =
                (SharkCurrencyComponentImpl) aliceSharkPeer
                        .getComponent(SharkCurrencyComponent.class);

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
            currencyComponent.establishGroup(membersToBeInvited,
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
    public void successfullGroupInviteSendAndReceived() throws SharkException {

        // 0. Set up Alice and Bob
        SharkPKITesthelper.incrementTestNumber();
        String folderName = SharkPKITesthelper.getPKITestFolder(ROOT_DIRECTORY);
        // Alice
        aliceSharkPeer = SharkPKITesthelper.setupSharkPeerDoNotStart(ALICE_NAME, folderName);
        SharkPKIComponent pkiForFactoryAlice
                = SharkPKITesthelper.setupPKIComponentPeerNotStarted(aliceSharkPeer, ALICE_ID);
        SharkCurrencyComponentFactory currencyFactoryAlice
                = new SharkCurrencyComponentFactory(pkiForFactoryAlice);
        aliceSharkPeer.addComponent(currencyFactoryAlice, SharkCurrencyComponent.class);
        aliceSharkPeer.start(ALICE_ID);
        // Bob
        bobSharkPeer = SharkPKITesthelper.setupSharkPeerDoNotStart(BOB_NAME, folderName);
        SharkPKIComponent pkiForFactoryBob
                = SharkPKITesthelper.setupPKIComponentPeerNotStarted(bobSharkPeer, BOB_ID);
        SharkCurrencyComponentFactory currencyFactoryBob
                = new SharkCurrencyComponentFactory(pkiForFactoryBob);
        bobSharkPeer.addComponent(currencyFactoryBob, SharkCurrencyComponent.class);
        aliceSharkPeer.start(BOB_ID);
    }
}
