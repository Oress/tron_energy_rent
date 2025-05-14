package org.ipan.nrgyrent.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;

@Entity
@Getter
@Setter
@Table(name = "nrg_deposit_transactions")
public class DepositTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_deposit_transactions_seq")
    @SequenceGenerator(name = "nrg_deposit_transactions_seq", sequenceName = "nrg_deposit_transactions_seq", allocationSize = 1)
    private Long id;

    @Column(name = "wallet_to", nullable = false)
    private String walletTo;

    @Column(name = "wallet_from", nullable = false)
    private String walletFrom;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "tx_id", nullable = false, unique = true)
    private String txId;

    @Column
    private Long timestamp;
}
