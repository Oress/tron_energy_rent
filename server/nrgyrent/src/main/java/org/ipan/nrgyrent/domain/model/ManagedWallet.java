package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "nrg_managed_wallets")
public class ManagedWallet {
    @Id
    @Column(name = "base58_address")
    private String base58Address;

    @Column(name = "private_key_enc")
    private byte[] privateKeyEncrypted;

    @CreationTimestamp
    private Instant createdAt;
}
