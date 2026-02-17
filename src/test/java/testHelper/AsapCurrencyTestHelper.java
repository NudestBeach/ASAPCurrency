package testHelper;

import currency.api.SharkCurrencyComponent;
import currency.api.SharkCurrencyComponentFactory;
import net.sharksystem.SharkException;
import net.sharksystem.SharkTestPeerFS;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.testhelper.SharkPKITesthelper;
import net.sharksystem.testhelper.SharkPeerTestHelper;
import testhelpers.SharkCurrencyComponentImpl;

import java.io.IOException;

import static net.sharksystem.utils.testsupport.TestConstants.ROOT_DIRECTORY;

/**
 * Helper class containing methods to set up test scenarios
 */
public class AsapCurrencyTestHelper extends SharkPeerTestHelper {

    private static int testNumber = 0;
    public final String subRootFolder;
    //private static int portNumber = 5000;
    public static int getPortNumber() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Kein freier Port konnte ermittelt werden.", e);
        }
    }

    public SharkTestPeerFS aliceSharkPeer;
    public SharkTestPeerFS bobSharkPeer;
    public SharkTestPeerFS claraSharkPeer;
    public SharkTestPeerFS davidSharkPeer;

    public SharkCurrencyComponent aliceCurrencyComponent;
    public SharkCurrencyComponent bobCurrencyComponent;
    public SharkCurrencyComponent claraCurrencyComponent;
    public SharkCurrencyComponent davidCurrencyComponent;

    public SharkCurrencyComponentImpl aliceImpl;
    public SharkCurrencyComponentImpl bobImpl;
    public SharkCurrencyComponentImpl claraImpl;
    public SharkCurrencyComponentImpl davidImpl;

    public AsapCurrencyTestHelper(String testVariant) {
        this.subRootFolder = ROOT_DIRECTORY + testVariant + "/";
    }

    public void runEncounter(SharkTestPeerFS leftPeer, SharkTestPeerFS rightPeer, boolean stop)
            throws SharkException, IOException, InterruptedException {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        System.out.println("                       start encounter: "
                + leftPeer.getASAPPeer().getPeerID() + " <--> " + rightPeer.getASAPPeer().getPeerID());
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

        leftPeer.getASAPTestPeerFS().startEncounter(getPortNumber(), rightPeer.getASAPTestPeerFS());
        // give them moment to exchange data
        Thread.sleep(1000);
        System.out.println("slept a moment");

        if(stop) {
            System.out.println("############################################################################");
            System.out.println("                   stop encounter: "
                    + leftPeer.getASAPPeer().getPeerID() + " <--> " + rightPeer.getASAPPeer().getPeerID());
            leftPeer.getASAPTestPeerFS().stopEncounter(rightPeer.getASAPTestPeerFS());
            System.out.println("############################################################################");
            Thread.sleep(100);
        }
    }

    /**
     * This just starts Alice not more
     * @throws SharkException Thrown when there are errors adding the component
     */
    public void setUpScenarioEstablishCurrency_1_justAlice() throws SharkException {
        this.aliceSharkPeer
                = new SharkTestPeerFS(ALICE_NAME, subRootFolder + "/" + ALICE_NAME);
        SharkPKIComponent pkiForFactory
                = SharkPKITesthelper.setupPKIComponentPeerNotStarted(this.aliceSharkPeer, ALICE_ID);
        SharkCurrencyComponentFactory currencyFactory
                = new SharkCurrencyComponentFactory(pkiForFactory);
        aliceSharkPeer.addComponent(currencyFactory, SharkCurrencyComponent.class);
        aliceSharkPeer.start(ALICE_ID);
        this.aliceCurrencyComponent
                = (SharkCurrencyComponent) this.aliceSharkPeer.getComponent(SharkCurrencyComponent.class);
        this.aliceImpl =
                (SharkCurrencyComponentImpl) aliceSharkPeer.getComponent(SharkCurrencyComponent.class);
        AsapCurrencyTestHelper.testNumber++;
    }

    public void setUpScenarioEstablishCurrency_2_BobAndAlice() throws SharkException {
        setUpScenarioEstablishCurrency_1_justAlice();
        this.bobSharkPeer
                = new SharkTestPeerFS(BOB_NAME, subRootFolder + "/" + BOB_NAME);
        SharkPKIComponent pkiForFactory
                = SharkPKITesthelper.setupPKIComponentPeerNotStarted(this.bobSharkPeer, BOB_ID);
        SharkCurrencyComponentFactory currencyFactory
                = new SharkCurrencyComponentFactory(pkiForFactory);
        bobSharkPeer.addComponent(currencyFactory, SharkCurrencyComponent.class);
        bobSharkPeer.start(BOB_ID);
        this.bobCurrencyComponent
                = (SharkCurrencyComponent) this.bobSharkPeer.getComponent(SharkCurrencyComponent.class);
        this.bobImpl =
                (SharkCurrencyComponentImpl) bobSharkPeer.getComponent(SharkCurrencyComponent.class);
        AsapCurrencyTestHelper.testNumber++;
    }

}
