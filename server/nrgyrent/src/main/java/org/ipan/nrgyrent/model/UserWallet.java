package org.ipan.nrgyrent.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class UserWallet {
    @Id
    private Long id;
    private String label;
    private String walletAddress;
    private Instant createdAt;
}
