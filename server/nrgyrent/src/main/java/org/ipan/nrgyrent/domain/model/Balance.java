package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.annotation.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "nrg_balances")
public class Balance {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_balance_seq")
    @SequenceGenerator(name = "nrg_balance_seq", sequenceName = "nrg_balance_seq", allocationSize = 1)
    private Long id;

    @Column(name = "deposit_address")
    private String depositAddress;

    private String label;

    @Enumerated(EnumType.STRING)
    private BalanceType type = BalanceType.INDIVIDUAL;

    @Column(name = "sun_balance")
    private Long sunBalance = 0L;

    @Column(name = "last_tx_id")
    private String lastTxId;

    @Column(name = "last_tx_timestamp")
    private Long lastTxTimestamp;


    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @Version
    private Long version;

    public Long makeDeposit(Long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        sunBalance += amount;
        return sunBalance;
    }
}
