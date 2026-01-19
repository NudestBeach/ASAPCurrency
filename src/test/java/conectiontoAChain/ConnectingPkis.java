package conectiontoAChain;

import Blockchain.Connecting;
import okhttp3.Credentials;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;

public class ConnectingPkis {

    @Test
    public void testConnectionToLocalChain() throws IOException {
        // 1. Arrange
        Connecting connector = new Connecting();

        // Wir erwarten, dass der Connector uns eine Web3j Instanz liefert
        // oder eine Methode hat, um die Verbindung zu prüfen.

        // 2. Act
        Web3j web3j = connector.connect("http://127.0.0.1:7545");

        // Prüfen, ob der Knoten antwortet (send() blockiert, bis Antwort kommt)
        String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();

        // 3. Assert
        System.out.println("Client Version: " + clientVersion);
        Assertions.assertNotNull(clientVersion, "Die Verbindung zur Blockchain konnte nicht hergestellt werden.");
        Assertions.assertTrue(clientVersion.contains("Ganache") || clientVersion.contains("Geth") || clientVersion.contains("Ethereum"),
                "Es scheint kein Ethereum-Client zu sein.");
    }


}
