package establishcurency;
import currency.api.SharkCurrency;
import currency.classes.Currency;
import currency.classes.LocalCurrency;
import exepections.ASAPCurrencyException;
import net.sharksystem.SharkException;
import net.sharksystem.SharkTestPeerFS;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.testhelper.SharkPKITesthelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static net.sharksystem.utils.testsupport.TestConstants.*;

public class EstablishCurrencyTests {
    SharkTestPeerFS aliceSharkPeer, bobSharkPeer;
    SharkPKIComponentImpl aliceComponent, bobComponent;

    private void setUpAndStartAliceAndBob() throws SharkException, InterruptedException {
        ////////////////////////////////////////// ALICE /////////////////////////////////////////////////////////
        /* it is a test - we use the test peer implementation
         only use SharkPeer interface in your application and create a SharkPeerFS instance
         That's for testing only
         */
        SharkPKITesthelper.incrementTestNumber();
        String folderName = SharkPKITesthelper.getPKITestFolder(ROOT_DIRECTORY);
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

        // 1. Alice arranges a new local Currency
        String currencyName = "AliceTaler";
        Currency dummyCurrency = new LocalCurrency(
                false,    // global limit
                new java.awt.List(),// centralized list (dummy)
                currencyName,       // Name
                "A test Currency"  // Spec
        );

        // 2. Alice creates a new Group using the created Currency
        // TODO: schlägt fehl, solange Impl leer ist
        this.aliceComponent.establishGroup(dummyCurrency, false, true);

        // 3. Assert: Check in ASAP Storage, if Channel exists
        net.sharksystem.asap.ASAPStorage currencyStorage =
                this.aliceSharkPeer.getASAPStorage(SharkCurrency.CURRENCY_FORMAT);

        // The URI should be the same as defined by the Currency Format
        CharSequence expetedURI = currencyName;

        // Check if Channel exists in Storage
        boolean channelExists = currencyStorage.channelExists(expetedURI);

        Assertions.assertTrue(channelExists, "The ASAP-Channel for the currency doesn't exist.");

    }

}
