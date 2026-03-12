package blockchain.wallet;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Objects;

/**
 * This is the Wallet for every ASAP Peer.
 *
 */
public class WalletManager {
    private Credentials credentials;

    // Lädt oder generiert ein neues Wallet für einen Peer
    public void initializeWallet(String password, File walletDirectory) throws InvalidAlgorithmParameterException, CipherException, NoSuchAlgorithmException, IOException, NoSuchProviderException {
        if(!walletDirectory.exists() || Objects.requireNonNull(walletDirectory.listFiles()).length == 0){
            String fileName = WalletUtils.generateNewWalletFile(password, walletDirectory);
            this.credentials = WalletUtils.loadCredentials(password, new File(walletDirectory, fileName));
        } else {
            File walletFile = Objects.requireNonNull(walletDirectory.listFiles())[0];
            this.credentials = WalletUtils.loadCredentials(password, walletFile);
        }
    }

    public String getMyAdress(){
        return credentials.getAddress();
    }

    public Credentials getCredentials(){
        return credentials;
    }
}
