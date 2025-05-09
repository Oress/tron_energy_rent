package org.ipan.nrgyrent.tron.wallet;


import org.bouncycastle.crypto.generators.SCrypt;
import org.ipan.nrgyrent.tron.crypto.SignInterface;
import org.ipan.nrgyrent.tron.exception.CipherException;
import org.ipan.nrgyrent.tron.utils.ByteArray;
import org.ipan.nrgyrent.tron.utils.Hash;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

/**
 * <p>Ethereum wallet file management. For reference, refer to <a href="https://github.com/ethereum/wiki/wiki/Web3-Secret-Storage-Definition">
 * Web3 Secret Storage Definition</a> or the <a href="https://github.com/ethereum/go-ethereum/blob/master/accounts/key_store_passphrase.go">
 * Go Ethereum client implementation</a>.</p>
 *
 * <p><strong>Note:</strong> the Bouncy Castle Scrypt implementation {@link SCrypt}, fails to comply
 * with the following Ethereum reference <a href="https://github.com/ethereum/wiki/wiki/Web3-Secret-Storage-Definition#scrypt">
 * Scrypt test vector</a>:</p>
 *
 * <pre>
 * {@code
 * // Only value of r that cost (as an int) could be exceeded for is 1
 * if (r == 1 && N_STANDARD > 65536)
 * {
 *     throw new IllegalArgumentException("Cost parameter N_STANDARD must be > 1 and < 65536.");
 * }
 * }
 * </pre>
 */
public class Wallet {

    private static final int N_LIGHT = 1 << 12;
    private static final int P_LIGHT = 6;

    private static final int N_STANDARD = 1 << 18;
    private static final int P_STANDARD = 1;

    private static final int R = 8;
    private static final int DKLEN = 32;

    private static final int CURRENT_VERSION = 3;

    private static final String CIPHER = "aes-128-ctr";
    static final String AES_128_CTR = "pbkdf2";
    static final String SCRYPT = "scrypt";

    public static WalletFile create(byte[] password, SignInterface ecKeySm2Pair, int n, int p)
            throws CipherException {

        byte[] salt = generateRandomBytes(32);

        byte[] derivedKey = generateDerivedScryptKey(password, salt, n, R, p, DKLEN);

        byte[] encryptKey = Arrays.copyOfRange(derivedKey, 0, 16);
        byte[] iv = generateRandomBytes(16);


        byte[] privateKeyBytes = ecKeySm2Pair.getPrivKeyBytes();

        byte[] cipherText = performCipherOperation(Cipher.ENCRYPT_MODE, iv, encryptKey, privateKeyBytes);

        byte[] mac = generateMac(derivedKey, cipherText);

        return createWalletFile(ecKeySm2Pair, cipherText, iv, salt, mac, n, p);
    }

    public static WalletFile createStandard(byte[] password, SignInterface ecKeySm2Pair) throws CipherException {
        return create(password, ecKeySm2Pair, N_STANDARD, P_STANDARD);
    }

    private static WalletFile createWalletFile(
            SignInterface ecKeySm2Pair, byte[] cipherText, byte[] iv, byte[] salt, byte[] mac,
            int n, int p) {

        WalletFile walletFile = new WalletFile();
        walletFile.setAddress(WalletApi.encode58Check(ecKeySm2Pair.getAddress()));

        WalletFile.Crypto crypto = new WalletFile.Crypto();
        crypto.setCipher(CIPHER);
        crypto.setCiphertext(ByteArray.toHexString(cipherText));
        walletFile.setCrypto(crypto);

        WalletFile.CipherParams cipherParams = new WalletFile.CipherParams();
        cipherParams.setIv(ByteArray.toHexString(iv));
        crypto.setCipherparams(cipherParams);

        crypto.setKdf(SCRYPT);
        WalletFile.ScryptKdfParams kdfParams = new WalletFile.ScryptKdfParams();
        kdfParams.setDklen(DKLEN);
        kdfParams.setN(n);
        kdfParams.setP(p);
        kdfParams.setR(R);
        kdfParams.setSalt(ByteArray.toHexString(salt));
        crypto.setKdfparams(kdfParams);

        crypto.setMac(ByteArray.toHexString(mac));
        walletFile.setCrypto(crypto);
        walletFile.setId(UUID.randomUUID().toString());
        walletFile.setVersion(CURRENT_VERSION);

        return walletFile;
    }

    private static byte[] generateDerivedScryptKey(
            byte[] password, byte[] salt, int n, int r, int p, int dkLen) throws CipherException {
        return SCrypt.generate(password, salt, n, r, p, dkLen);
    }

    private static byte[] performCipherOperation(int mode, byte[] iv, byte[] encryptKey, byte[] text) throws CipherException {
        try {
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");

            SecretKeySpec secretKeySpec = new SecretKeySpec(encryptKey, "AES");
            cipher.init(mode, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(text);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                 | InvalidAlgorithmParameterException | InvalidKeyException
                 | BadPaddingException | IllegalBlockSizeException e) {
            throw new CipherException("Error performing cipher operation", e);
        }
    }

    private static byte[] generateMac(byte[] derivedKey, byte[] cipherText) {
        byte[] result = new byte[16 + cipherText.length];

        System.arraycopy(derivedKey, 16, result, 0, 16);
        System.arraycopy(cipherText, 0, result, 16, cipherText.length);

        return Hash.sha3(result);
    }

    public static byte[] generateRandomBytes(int size) {
        byte[] bytes = new byte[size];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

}
