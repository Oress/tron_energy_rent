package org.ipan.nrgyrent;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import org.bouncycastle.util.encoders.Hex;
import org.ipan.nrgyrent.domain.service.ManagedWalletService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NrgyrentApplication {
    public static void main(String[] args) throws IOException {
        SpringApplication.run(NrgyrentApplication.class, args);

//         byte[] key = new byte[32]; // 32 bytes = 256 bits for AES-256
//         new SecureRandom().nextBytes(key);
//         String base64Key = Base64.getEncoder().encodeToString(key);
//         System.out.println("Base64 AES key: " + base64Key);

//        byte[] key = Base64.getDecoder().decode("");
//        byte[] privateKey = ManagedWalletService.decrypt(Hex.decode(""), key);
//        System.out.println("Private key: " + Hex.toHexString(privateKey));
    }
}
