package conectiontoAChain;

import currencyGroupTests.CurrencyGroupTests;
import net.sharksystem.SharkException;
import org.junit.jupiter.api.*;
import testHelper.AsapCurrencyTestHelper;

public class CryptoWalletTest extends  AsapCurrencyTestHelper{

    public CryptoWalletTest() {
        super(CryptoWalletTest.class.getSimpleName());
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
    public void testOnePeerHasAnEthereumWallet() throws SharkException {
        this.setUpScenarioEstablishCurrency_1_justAlice();

        // Check if Alice have a wallet
        Assertions.assertNotNull(this.aliceImpl.getWallet(), "Alice doesn't have a wallet!");
        Assertions.assertFalse(this.aliceImpl.getWalletAddress().isEmpty(), "Alice Wallet Address is empty!");
        System.out.println("Alice Wallet: " + this.aliceImpl.getWalletAddress());
    }

    @Test
    public void testEveryPeerHasAnEthereumWallet() throws SharkException {
        this.setUpScenarioEstablishCurrency_4_DavidAndClaraAndBobAndAlice();

        Assertions.assertNotNull(this.aliceImpl.getWallet(), "Alice doesn't have a wallet!");
        Assertions.assertFalse(this.aliceImpl.getWalletAddress().isEmpty(), "Alice Wallet Address is empty!");
        System.out.println("Alice Wallet: " + this.aliceImpl.getWalletAddress());

        Assertions.assertNotNull(this.bobImpl.getWallet(), "Bob doesn't have a wallet!");
        Assertions.assertFalse(this.bobImpl.getWalletAddress().isEmpty(), "Bob Wallet Address is empty!");
        System.out.println("Bobs Wallet: " + this.bobImpl.getWalletAddress());

        Assertions.assertNotNull(this.claraImpl.getWallet(), "Clara doesn't have a wallet!");
        Assertions.assertFalse(this.claraImpl.getWalletAddress().isEmpty(), "Claras Wallet Address is empty!");
        System.out.println("Clara Wallet: " + this.claraImpl.getWalletAddress());

        Assertions.assertNotNull(this.davidImpl.getWallet(), "David doesn't have a wallet!");
        Assertions.assertFalse(this.davidImpl.getWalletAddress().isEmpty(), "Davids Wallet Address is empty!");
        System.out.println("David Wallet: " + this.davidImpl.getWalletAddress());

        // Check if the Addresses are different
        Assertions.assertNotEquals(aliceImpl.getWalletAddress(), this.bobImpl.getWalletAddress());
        Assertions.assertNotEquals(bobImpl.getWalletAddress(), this.claraImpl.getWalletAddress());
        Assertions.assertNotEquals(claraImpl.getWalletAddress(), this.davidImpl.getWalletAddress());
    }
}
