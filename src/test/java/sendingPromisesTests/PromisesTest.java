package sendingPromisesTests;

import currencyGroupTests.CurrencyGroupTests;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testHelper.AsapCurrencyTestHelper;

import java.io.File;
import java.io.IOException;

public class PromisesTest extends AsapCurrencyTestHelper {

    public PromisesTest() {
        super(PromisesTest.class.getSimpleName());
    }

    @BeforeEach
    void setUp() {
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
    }

    @AfterEach
    void tearDown() {
        stopPeerSafely(this.aliceSharkPeer);
        stopPeerSafely(this.bobSharkPeer);
        stopPeerSafely(this.claraSharkPeer);
        stopPeerSafely(this.davidSharkPeer);
    }

    @Test
    public void createPromise() {}

    @Test
    public void createPromiseAndSendWithinAGroup() {}

    @Test
    public void sendPromiseWithouGroup() {}




}
