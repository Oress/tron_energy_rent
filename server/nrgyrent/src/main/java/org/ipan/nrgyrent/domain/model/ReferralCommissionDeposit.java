package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

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

@Entity
@Getter
@Setter
@Table(name = "nrg_referral_commission_deposits")
public class ReferralCommissionDeposit {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_referral_commission_deposits_seq")
    @SequenceGenerator(name = "nrg_referral_commission_deposits_seq", sequenceName = "nrg_referral_commission_deposits_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "to_balance_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_nrg_referral_commission_deposits_to_balance_id"))
    private Balance balance;

    @Column(name="amount_sun")
    private Long amountSun;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
