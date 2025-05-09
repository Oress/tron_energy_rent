package org.ipan.nrgyrent.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class AppUser {
    @Id
    private Long telegramId;

//    @OneToOne
//    private DepositWallet depositWallet;

    private boolean isActive;

    private Instant createdAt;
}
