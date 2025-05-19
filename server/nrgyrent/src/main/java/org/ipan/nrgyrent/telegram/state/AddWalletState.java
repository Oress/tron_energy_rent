package org.ipan.nrgyrent.telegram.state;

public interface AddWalletState {
    String getAddress();

    AddWalletState withAddress(String value);
}
