package establishcurency;
import Group.SharkGroupDocument;
import currency.api.SharkCurrencyComponent;
import currency.api.SharkCurrencyComponentFactory;
import currency.classes.Currency;
import currency.classes.LocalCurrency;
import exepections.ASAPCurrencyException;
import net.sharksystem.SharkException;
import net.sharksystem.SharkPeerFS;
import net.sharksystem.SharkTestPeerFS;
import net.sharksystem.asap.ASAPChannel;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeerFS;
import net.sharksystem.asap.ASAPStorage;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.asap.engine.ASAPEngine;
import net.sharksystem.asap.engine.ASAPEngineFS;
import net.sharksystem.fs.FSUtils;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.pki.SharkPKIComponentFactory;
import net.sharksystem.testhelper.SharkPKITesthelper;
import net.sharksystem.utils.SerializationHelper;
import net.sharksystem.utils.testsupport.TestConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testhelpers.SharkCurrencyComponentImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.sharksystem.utils.testsupport.TestConstants.*;

public class EstablishCurrencyTests {

    private String TEST_FOLDER;
    private final CharSequence EXAMPLE_APP_FORMAT = "shark/x-establishCurrencyExample";
    static final CharSequence FORMAT = "TestFormat";

    SharkTestPeerFS aliceSharkPeer, bobSharkPeer;
    SharkPKIComponentImpl aliceComponent, bobComponent;

    // Set-Up a Folder within out project to save testing data
    @BeforeEach
    public void init() {
        // get current user dir
        String currentDir = System.getProperty("user.dir");
        TEST_FOLDER = currentDir + "/ASAPCurrencyTestFolder";
        // delete test dir if already exists
        FSUtils.removeFolder(TEST_FOLDER);
    }

    private void setUpAndStartAliceAndBob() throws SharkException, InterruptedException {
        ////////////////////////////////////////// ALICE /////////////////////////////////////////////////////////
        /* it is a test - we use the test peer implementation
         only use SharkPeer interface in your application and create a SharkPeerFS instance
         That's for testing only
         */
        SharkPKITesthelper.incrementTestNumber();
        CharSequence folderName = SharkPKITesthelper.getPKITestFolder(ROOT_DIRECTORY);
        System.out.println("folderName == " + folderName);

        ///////// Alice
        aliceSharkPeer = SharkPKITesthelper.setupSharkPeerDoNotStart(ALICE_NAME, folderName);
        aliceComponent =
                (SharkPKIComponentImpl) SharkPKITesthelper.setupPKIComponentPeerNotStarted(aliceSharkPeer, ALICE_ID);
        aliceSharkPeer.start(ALICE_ID);

        ////////////////////////////////////////// BOB ///////////////////////////////////////////////////////////
        bobSharkPeer = SharkPKITesthelper.setupSharkPeerDoNotStart(BOB_NAME, folderName);
        bobComponent = (SharkPKIComponentImpl)
                SharkPKITesthelper.setupPKIComponentPeerNotStarted(bobSharkPeer, BOB_ID);
        bobSharkPeer.start(BOB_ID);

        Thread.sleep(200);
    }

    @Test
    public void aliceCreatesAGroupWithLocalCurrency()
            throws SharkException, ASAPException, ASAPCurrencyException, IOException {

        // 0. Setting up Alice Peer
        Collection<CharSequence> formats = new ArrayList<>();
        CharSequence aliceFolder = TEST_FOLDER + "/" + TestConstants.ALICE_ID;
        SharkPeerFS alice = new SharkPeerFS(TestConstants.ALICE_ID, aliceFolder);
        formats.add(EXAMPLE_APP_FORMAT);
        SharkPKIComponentFactory certificateComponentFactory = new SharkPKIComponentFactory();
        alice.addComponent(certificateComponentFactory, SharkPKIComponent.class);
        SharkCurrencyComponentFactory currencyFactory = new SharkCurrencyComponentFactory(
                (SharkPKIComponent) alice.getComponent(SharkPKIComponent.class));
        ASAPPeerFS aliceAsapPeer = new ASAPPeerFS(ALICE_NAME, aliceFolder);
        alice.addComponent(currencyFactory, SharkCurrencyComponent.class);
        alice.start(aliceAsapPeer);

        // 1. Alice arranges a new local Currency
        CharSequence currencyName = "AliceTaler";
        Currency dummyCurrency = new LocalCurrency(
                false,                // global limit
                new ArrayList(),                // centralized list (dummy)
                currencyName.toString(),        // Name
                "A test Currency"               // Spec
        );

        // 2. Alice creates a new Group using the created Currency
        SharkCurrencyComponentImpl currencyComponent =
                (SharkCurrencyComponentImpl) alice.getComponent(SharkCurrencyComponent.class);
        currencyComponent.establishGroup(dummyCurrency, new ArrayList<CharSequence>(), false, true);
        SharkGroupDocument testDoc = currencyComponent.getSharkGroupDocument(currencyName);
        byte[] groupId = testDoc.getGroupId();
        byte[] aliceSignature = testDoc.getCurrentMembers().get(ALICE_NAME);

        // 3. Checking results
        boolean channelExists = alice.getASAPPeer()
                .getASAPStorage(SharkCurrencyComponent.CURRENCY_FORMAT)
                .channelExists(currencyName);
        boolean verified = ASAPCryptoAlgorithms.verify(
                groupId, aliceSignature, ALICE_NAME, ((SharkPKIComponent) alice.getComponent(SharkPKIComponent.class)).getASAPKeyStore());
        Assertions.assertEquals(ALICE_NAME,testDoc.getGroupCreator());
        Assertions.assertTrue(channelExists, "Channel does not exist");
        Assertions.assertTrue(verified, "The Signature of Alice could not have been verified");

    }

}
