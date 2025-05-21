package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@Table(name = "nrg_withdrawal_orders")
public class WithdrawalOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_withdrawal_orders_seq")
    @SequenceGenerator(name = "nrg_withdrawal_orders_seq", sequenceName = "nrg_withdrawal_orders_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "telegram_id", foreignKey = @ForeignKey(name = "fk_nrg_orders_user_id"))
    private AppUser user;

    @ManyToOne
    @JoinColumn(name = "balance_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_nrg_orders_balance_id"))
    private Balance balance;

    @Column(name = "sun_amount")
    private Long sunAmount;

    @Column(name = "fee_sun_amount")
    private Long feeSunAmount;

    @Column(name = "receive_address")
    private String receiveAddress;

    @Enumerated(EnumType.STRING)
    private WithdrawalStatus status;

    @Column(name = "tx_id")
    private String txId;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
