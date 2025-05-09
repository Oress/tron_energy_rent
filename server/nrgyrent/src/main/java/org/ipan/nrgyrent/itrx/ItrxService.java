package org.ipan.nrgyrent.itrx;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ItrxService {
    private final RestClient restClient;

    public void placeOrder(Integer energyAmount,String receiveAddress) {
        restClient.placeOrder(energyAmount, "1H", receiveAddress, "1234567890");
    }
}
