package org.ipan.nrgyrent.tron.node.events;

import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AddressesWatchlist {
    private final ConcurrentHashSet<String> addresses = new ConcurrentHashSet<>();
    private final BalanceRepo balanceRepo;

    @PostConstruct
    public void initWatchList() {
        addresses.addAll(this.balanceRepo.findAllActiveAddresses());
    }

    // WHEN: on startup, on balance created
    public void addAddress(String depositAddress) {
        if (depositAddress != null && !depositAddress.isEmpty()) {
            addresses.add(depositAddress);
        }
    }

    public boolean contains(String address) {
        return address != null && addresses.contains(address);
    }
}
