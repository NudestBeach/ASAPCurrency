package blockchain.transaction;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

public class OfflineTXCreator {

    /**
     * Creates a signed offline transaction, which will be returned as Hex-String.
     * @param credentials Credentials of the own Ethereum Wallet
     * @param toAddress Address of the recipient
     * @param amountInWei amount of the Ethereum Currency we want to send
     * @param nonce Nonce value of the transaction
     * @param gasPrice price willing to pay per unit of gas consumed
     * @param gasLimit maximal limit on the number of gas units that this transaction may consume
     * @return transaction as Hex-String (which will be gossiped with ASAP)
     */
    public static String createSignedOfflineTX(Credentials credentials,
                                               String toAddress,
                                               BigInteger amountInWei,
                                               BigInteger nonce,
                                               BigInteger gasPrice,
                                               BigInteger gasLimit){
        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, amountInWei);
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        return Numeric.toHexString(signedMessage);
    }
}
