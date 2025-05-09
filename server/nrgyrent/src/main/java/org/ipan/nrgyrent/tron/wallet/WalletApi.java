package org.ipan.nrgyrent.tron.wallet;

import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.tron.crypto.ECKey;
import org.ipan.nrgyrent.tron.crypto.Sha256Sm3Hash;
import org.ipan.nrgyrent.tron.crypto.SignInterface;
import org.ipan.nrgyrent.tron.exception.CipherException;
import org.ipan.nrgyrent.tron.mnemonic.Mnemonic;
import org.ipan.nrgyrent.tron.mnemonic.MnemonicFile;
import org.ipan.nrgyrent.tron.mnemonic.MnemonicUtils;
import org.ipan.nrgyrent.tron.utils.Base58;
import org.ipan.nrgyrent.tron.utils.ByteArray;
import org.ipan.nrgyrent.tron.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;



@Slf4j
public class WalletApi {

    public static String pkFromMnemonic(List<String> words) {
        byte[] priKey = MnemonicUtils.getPrivateKeyFromMnemonic(words);
        return ByteArray.toHexString(priKey);
    }

    public static String hexAddressFromMnemonic(List<String> words) {
        byte[] priKey = MnemonicUtils.getPrivateKeyFromMnemonic(words);
        ECKey ecKey = new ECKey(priKey, true);
        byte[] address =  ecKey.getAddress();
        return ByteArray.toHexString(address);
    }

    public static String base58AddressFromMnemonic(List<String> words) {
        byte[] priKey = MnemonicUtils.getPrivateKeyFromMnemonic(words);
        ECKey ecKey = new ECKey(priKey, true);
        byte[] address =  ecKey.getAddress();
        return WalletApi.encode58Check(address);
    }

    /**
     * Creates a new WalletApi with a random ECKey or no ECKey.
     */
    public static WalletFile CreateWalletFile(byte[] password, int wordsNumber) throws IOException {
        WalletFile walletFile = null;
        SecureRandom secureRandom = Utils.getRandom();
        try {
            List<String> mnemonicWords = MnemonicUtils.generateMnemonic(secureRandom, wordsNumber);
            byte[] priKey = MnemonicUtils.getPrivateKeyFromMnemonic(mnemonicWords);

            ECKey ecKey = new ECKey(priKey, true);
            walletFile = Wallet.createStandard(password, ecKey);
//            storeMnemonicWords(password, ecKey, mnemonicWords);

            System.out.println("mnemonic words : " + MnemonicUtils.mnemonicWordsToString(mnemonicWords));
            System.out.println("private key : " + ByteArray.toHexString(priKey));
            System.out.println("address : " + walletFile.getAddress());

            Arrays.fill(priKey, (byte) 0);
            for (int i = 0; i < mnemonicWords.size(); i++) {
                mnemonicWords.set(i, null);
            }
        } catch (Exception e) {
            throw new IOException("Mnemonic generation failed", e);
        }

        return walletFile;
    }

    public static void storeMnemonicWords(byte[] password, SignInterface ecKeySm2Pair, List<String> mnemonicWords) throws CipherException, IOException {
        MnemonicFile mnemonicFile = Mnemonic.createStandard(password, ecKeySm2Pair, mnemonicWords);
        String keystoreName = MnemonicUtils.store2Keystore(mnemonicFile);
        System.out.println("mnemonic file : ."
                + File.separator + "Mnemonic" + File.separator
                + keystoreName);
    }

    public static String encode58Check(byte[] input) {
        byte[] hash0 = Sha256Sm3Hash.hash(input);
        byte[] hash1 = Sha256Sm3Hash.hash(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }
}
