package sendingPromisesTests;

import currency.classes.SharkCurrency;
import currency.classes.SharkLocalCurrency;
import currencyGroupTests.CurrencyGroupTests;
import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.pki.CredentialMessageInMemo;
import net.sharksystem.pki.SharkPKIComponent;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testHelper.AsapCurrencyTestHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PromisesTest extends AsapCurrencyTestHelper {

    public PromisesTest() {
        super(PromisesTest.class.getSimpleName());
    }

    @BeforeEach
    void setUp() throws SharkException, InterruptedException, IOException {
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
        //Establish Group with 2 Users

        this.setUpScenarioEstablishCurrency_2_BobAndAlice();


        CharSequence currencyName = "AliceTalerC";
        SharkCurrency dummyCurrency = new SharkLocalCurrency(
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


    }

    @AfterEach
    void tearDown() {
        stopPeerSafely(this.aliceSharkPeer);
        stopPeerSafely(this.bobSharkPeer);
        stopPeerSafely(this.claraSharkPeer);
        stopPeerSafely(this.davidSharkPeer);
    }

    @Test
    public void createPromise() throws SharkException, IOException {

        SharkPKIComponent alicePKI = (SharkPKIComponent) this.aliceSharkPeer.getComponent(SharkPKIComponent.class);
        SharkPKIComponent bobPKI = (SharkPKIComponent) this.bobSharkPeer.getComponent(SharkPKIComponent.class);

        // let Bob accept ALice credentials and create a certificate
        CredentialMessageInMemo aliceCredentialMessage = new CredentialMessageInMemo(ALICE_ID, ALICE_NAME, System.currentTimeMillis(), alicePKI.getPublicKey());
        bobPKI.acceptAndSignCredential(aliceCredentialMessage);

        // Alice accepts Bob Public Key
        CredentialMessageInMemo bobCredentialMessage = new CredentialMessageInMemo(BOB_ID, BOB_NAME, System.currentTimeMillis(), bobPKI.getPublicKey());
        alicePKI.acceptAndSignCredential(bobCredentialMessage);

//        this.aliceComponent = (SharkCreditMoneyComponent) this.alicePeer.getComponent(SharkCreditMoneyComponent.class);
//        this.bobComponent = (SharkCreditMoneyComponent) this.bobPeer.getComponent(SharkCreditMoneyComponent.class);
//
//        // Add SharkBondReceivedListener Implementation
//        SharkBondsReceivedListener aliceListener = new DummySharkBondReceivedListener(this.aliceComponent);
//        this.aliceComponent.subscribeBondReceivedListener(aliceListener);
//
//        SharkBondsReceivedListener bobListener = new DummySharkBondReceivedListener(this.bobComponent);
//        this.bobComponent.subscribeBondReceivedListener(bobListener);
//
//        // set up backdoors
//        this.aliceComponentImpl = (SharkCreditMoneyComponentImpl) this.aliceComponent;
//        this.bobComponentImpl = (SharkCreditMoneyComponentImpl) this.bobComponent;




    }

    @Test
    public void createPromiseAndSendWithinAGroup() {}

    @Test
    public void sendPromiseWithouGroup() {}




}
