package Blockchain;

import java.math.BigInteger;
//TODO ist noch nicht eingunden ins projekt,
public interface BlockchainConnector {
    /**
     * Checks, if a Wallet exists and if it has enough Bblance
     * @param walletAdress adress of the user
     * @param amount How much you need
     * @return true, if its enough
     */
    boolean verifyFunds(String walletAdress, BigInteger amount);
}
