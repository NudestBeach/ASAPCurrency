package establishcurency;
import Group.SharkGroupDocument;
import currency.api.SharkCurrencyComponent;
import currency.api.SharkCurrencyComponentFactory;
import currency.classes.Currency;
import currency.classes.LocalCurrency;
import exepections.ASAPCurrencyException;
import net.sharksystem.SharkException;
import net.sharksystem.SharkTestPeerFS;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.fs.FSUtils;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.testhelper.SharkPKITesthelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testhelpers.SharkCurrencyComponentImpl;

import java.io.IOException;
import java.util.*;

import static net.sharksystem.utils.testsupport.TestConstants.*;

public class EstablishCurrencyTests {

    SharkTestPeerFS aliceSharkPeer;

    @Test
    public void aliceCreatesAGroupWithLocalCurrency()
            throws SharkException, IOException {

        // 0. Setting up Alice Peer
        SharkPKITesthelper.incrementTestNumber();
        String folderName = SharkPKITesthelper.getPKITestFolder(ROOT_DIRECTORY);
        aliceSharkPeer = SharkPKITesthelper.setupSharkPeerDoNotStart(ALICE_NAME, folderName);
        SharkPKIComponent pkiForFactory = SharkPKITesthelper.setupPKIComponentPeerNotStarted(aliceSharkPeer, ALICE_ID);
        SharkCurrencyComponentFactory currencyFactory = new SharkCurrencyComponentFactory(pkiForFactory);
        aliceSharkPeer.addComponent(currencyFactory, SharkCurrencyComponent.class);
        aliceSharkPeer.start(ALICE_ID);

        SharkCurrencyComponentImpl currencyComponent =
                (SharkCurrencyComponentImpl) aliceSharkPeer.getComponent(SharkCurrencyComponent.class);

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTalerA";
        Currency dummyCurrency = new LocalCurrency(
                false,                // global limit
                new ArrayList<>(),                // centralized list (dummy)
                currencyName.toString(),        // Name
                "A test Currency"               // Spec
        );

        // 2. Alice creates a new Group using the created Currency
        currencyComponent.establishGroup(dummyCurrency, new ArrayList<CharSequence>(), false, true);
        SharkGroupDocument testDoc = currencyComponent.getSharkGroupDocument(currencyName);
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
                ((SharkPKIComponent) aliceSharkPeer.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
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
        SharkPKIComponent pkiForFactory = SharkPKITesthelper.setupPKIComponentPeerNotStarted(aliceSharkPeer, ALICE_ID);
        SharkCurrencyComponentFactory currencyFactory = new SharkCurrencyComponentFactory(pkiForFactory);
        aliceSharkPeer.addComponent(currencyFactory, SharkCurrencyComponent.class);
        aliceSharkPeer.start(ALICE_ID);

        SharkCurrencyComponentImpl currencyComponent =
                (SharkCurrencyComponentImpl) aliceSharkPeer.getComponent(SharkCurrencyComponent.class);

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
        currencyComponent.establishGroup(dummyCurrency, new ArrayList<CharSequence>(), false, true);
        SharkGroupDocument testDoc = currencyComponent.getSharkGroupDocument(currencyName);
        byte[] groupId = testDoc.getGroupId();
        byte[] aliceSignature = testDoc.getCurrentMembers().get(ALICE_ID);

    }
}
