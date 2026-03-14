package sendingPromisesTests;

import currency.classes.*;
import net.sharksystem.SharkException;
import net.sharksystem.asap.pki.CredentialMessageInMemo;
import net.sharksystem.pki.SharkPKIComponent;
import org.junit.jupiter.api.*;
import testHelper.AsapCurrencyTestHelper;

import java.io.IOException;

public class PromisesTest extends AsapCurrencyTestHelper {

    public PromisesTest() {
        super(PromisesTest.class.getSimpleName());
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        String testName = testInfo.getDisplayName()
                .replaceAll("[^a-zA-Z0-9]", "_");
        this.initSubRootFolder(
                PromisesTest.class.getSimpleName(),
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
    public void createPromiseAndSendToBob() throws SharkException, IOException, InterruptedException {

        // Alice created a group with bob in it (he accepted). This method returns the groupID
        byte[] groupId = this.aliceCreatesGroupWithBobSetUp();

        SharkPKIComponent alicePKI = (SharkPKIComponent) this.aliceSharkPeer.getComponent(SharkPKIComponent.class);
        SharkPKIComponent bobPKI = (SharkPKIComponent) this.bobSharkPeer.getComponent(SharkPKIComponent.class);

        // let Bob accept ALice credentials and create a certificate
        CredentialMessageInMemo aliceCredentialMessage = new CredentialMessageInMemo(ALICE_ID, ALICE_NAME, System.currentTimeMillis(), alicePKI.getPublicKey());
        bobPKI.acceptAndSignCredential(aliceCredentialMessage);

        // Alice accepts Bob Public Key
        CredentialMessageInMemo bobCredentialMessage = new CredentialMessageInMemo(BOB_ID, BOB_NAME, System.currentTimeMillis(), bobPKI.getPublicKey());
        alicePKI.acceptAndSignCredential(bobCredentialMessage);

        CharSequence promiseId = this.aliceCurrencyComponent.createPromise(2,
                this.aliceStorage.getGroupDocument(groupId).getAssignedCurrency(),
                groupId,
                ALICE_ID, //creditor
                BOB_ID, //debtor
                true);

        Thread.sleep(200);
        this.runEncounter(this.aliceSharkPeer, this.bobSharkPeer, true);
        Thread.sleep(200);

        //we are also testing for bobs side because create triggers sending a promise
        SharkPromise alicePromise = this.aliceStorage.getSharkSignedPromiseFromStorage(promiseId);
        SharkPromise bobPromise = this.bobStorage.getSharkSignedPromiseFromStorage(promiseId);


        Assertions.assertNotNull(alicePromise);
        Assertions.assertNotNull(bobPromise);
        Assertions.assertNotNull(promiseId);
        Assertions.assertEquals(promiseId, alicePromise.getPromiseID());
        Assertions.assertEquals(promiseId, bobPromise.getPromiseID());
        Assertions.assertEquals(SharkPromiseSignings.SIGNED_BY_CREDITOR
                ,alicePromise.getSigningStateOfPromise());
        Assertions.assertEquals(SharkPromiseSignings.SIGNED_BY_CREDITOR
                ,bobPromise.getSigningStateOfPromise());
        Assertions.assertNull(alicePromise.getDebtorSignature());
        Assertions.assertNotNull(alicePromise.getCreditorSignature());
        Assertions.assertNull(bobPromise.getDebtorSignature());
        Assertions.assertNotNull(bobPromise.getCreditorSignature());
    }

    @Test
    public void createPromiseAndSendWithinAGroup() {}

    @Test
    public void sendPromiseWithouGroup() {}




}
