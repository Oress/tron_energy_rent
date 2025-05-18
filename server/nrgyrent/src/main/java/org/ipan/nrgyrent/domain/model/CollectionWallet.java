package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "nrg_collection_wallets")
public class CollectionWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_collection_wallets_seq")
    @SequenceGenerator(name = "nrg_collection_wallets_seq", sequenceName = "nrg_collection_wallets_seq", allocationSize = 1)
    private Long id;

    @Column(name = "wallet_address")
    private String walletAddress;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
