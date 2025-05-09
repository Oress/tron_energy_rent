package org.ipan.nrgyrent.tron.mnemonic;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.params.KeyParameter;
import org.ipan.nrgyrent.tron.crypto.SignInterface;
import org.ipan.nrgyrent.tron.exception.CipherException;
import org.ipan.nrgyrent.tron.utils.ByteArray;
import org.ipan.nrgyrent.tron.utils.Hash;
import org.ipan.nrgyrent.tron.wallet.WalletApi;

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
import java.util.List;
import java.util.UUID;

public class Mnemonic {

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

  public static MnemonicFile create(byte[] password, SignInterface ecKeySm2Pair,
                                    String mnemonicWords, int n, int p)
      throws CipherException {
    byte[] salt = generateRandomBytes(32);
    byte[] derivedKey = generateDerivedScryptKey(password, salt, n, R, p, DKLEN);
    byte[] encryptKey = Arrays.copyOfRange(derivedKey, 0, 16);
    byte[] iv = generateRandomBytes(16);
    byte[] mnemonicWordsBytes = mnemonicWords.getBytes();
    byte[] cipherText = performCipherOperation(Cipher.ENCRYPT_MODE, iv, encryptKey,
        mnemonicWordsBytes);
    byte[] mac = generateMac(derivedKey, cipherText);
    return createMnemonicFile(ecKeySm2Pair, cipherText, iv, salt, mac, n, p);
  }

  public static MnemonicFile createStandard(byte[] password, SignInterface ecKeySm2Pair, List<String> mnemonicWords)
      throws CipherException {
    return create(password, ecKeySm2Pair, MnemonicUtils.mnemonicWordsToString(mnemonicWords), N_STANDARD, P_STANDARD);
  }

  public static MnemonicFile createLight(byte[] password, SignInterface ecKeySm2Pair, List<String> mnemonicWords)
      throws CipherException {
    return create(password, ecKeySm2Pair, MnemonicUtils.mnemonicWordsToString(mnemonicWords), N_LIGHT, P_LIGHT);
  }

  private static MnemonicFile createMnemonicFile(SignInterface ecKeySm2Pair,
                                                 byte[] cipherText, byte[] iv, byte[] salt,
                                                 byte[] mac, int n, int p) {
    MnemonicFile MnemonicFile = new MnemonicFile();
    MnemonicFile.setAddress(WalletApi.encode58Check(ecKeySm2Pair.getAddress()));

    MnemonicFile.Crypto crypto = new MnemonicFile.Crypto();
    crypto.setCipher(CIPHER);
    crypto.setCiphertext(ByteArray.toHexString(cipherText));
    MnemonicFile.setCrypto(crypto);

    MnemonicFile.CipherParams cipherParams = new MnemonicFile.CipherParams();
    cipherParams.setIv(ByteArray.toHexString(iv));
    crypto.setCipherparams(cipherParams);

    crypto.setKdf(SCRYPT);
    MnemonicFile.ScryptKdfParams kdfParams = new MnemonicFile.ScryptKdfParams();
    kdfParams.setDklen(DKLEN);
    kdfParams.setN(n);
    kdfParams.setP(p);
    kdfParams.setR(R);
    kdfParams.setSalt(ByteArray.toHexString(salt));
    crypto.setKdfparams(kdfParams);

    crypto.setMac(ByteArray.toHexString(mac));
    MnemonicFile.setCrypto(crypto);
    MnemonicFile.setId(UUID.randomUUID().toString());
    MnemonicFile.setVersion(CURRENT_VERSION);

    return MnemonicFile;
  }

  private static byte[] generateDerivedScryptKey(
      byte[] password, byte[] salt, int n, int r, int p, int dkLen) throws CipherException {
    return SCrypt.generate(password, salt, n, r, p, dkLen);
  }

  private static byte[] generateAes128CtrDerivedKey(
      byte[] password, byte[] salt, int c, String prf) throws CipherException {

    if (!prf.equals("hmac-sha256")) {
      throw new CipherException("Unsupported prf:" + prf);
    }

    PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
    gen.init(password, salt, c);
    return ((KeyParameter) gen.generateDerivedParameters(256)).getKey();
  }

  private static byte[] performCipherOperation(
      int mode, byte[] iv, byte[] encryptKey, byte[] text) throws CipherException {

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
