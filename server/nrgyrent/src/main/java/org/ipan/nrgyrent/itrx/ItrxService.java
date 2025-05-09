package org.ipan.nrgyrent.itrx;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ItrxService {
    private final RestClient restClient;

    public void placeOrder(String receiveAddress) {
        restClient.placeOrder(65_000, "1H", receiveAddress, "1234567890");
    }
}
