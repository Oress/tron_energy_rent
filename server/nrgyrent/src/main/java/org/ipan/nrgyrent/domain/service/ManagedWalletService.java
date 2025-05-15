package org.ipan.nrgyrent.domain.service;

import java.io.IOException;

import org.ipan.nrgyrent.domain.model.ManagedWallet;
import org.ipan.nrgyrent.tron.crypto.ECKey;
import org.ipan.nrgyrent.tron.wallet.WalletApi;
import org.springframework.stereotype.Service;

@Service
public class ManagedWalletService {

    // TODO: Encrypt the private key
    public ManagedWallet generateDepositWallet() throws IOException {
        ManagedWallet managedWallet = new ManagedWallet();

        ECKey privateKeyForNewWallet = WalletApi.generatePrivateKeyForNewWallet();
        byte[] address = privateKeyForNewWallet.getAddress();
        managedWallet.setBase58Address(WalletApi.encode58Check(address));
        managedWallet.setPrivateKeyEncrypted(privateKeyForNewWallet.getPrivateKey());

        return managedWallet;
    }
}
