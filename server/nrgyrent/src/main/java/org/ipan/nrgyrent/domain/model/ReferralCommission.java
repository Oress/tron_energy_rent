package org.ipan.nrgyrent.domain.model;

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
@Table(name = "nrg_referral_commissions")
public class ReferralCommission {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_referral_commissions_seq")
    @SequenceGenerator(name = "nrg_referral_commissions_seq", sequenceName = "nrg_referral_commissions_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_nrg_referral_commissions_order_id"))
    private Order order;

    @ManyToOne
    @JoinColumn(name = "bal_ref_prog_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_nrg_referral_commissions_bal_ref_prog_id"))
    private BalanceReferralProgram balanceReferralProgram;

    @ManyToOne
    @JoinColumn(name = "ref_prog_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_nrg_referral_commissions_ref_program_id"))
    private ReferralProgram referralProgram;

    @ManyToOne
    @JoinColumn(name = "deposit_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_nrg_referral_commissions_deposit_id"))
    private ReferralCommissionDeposit deposit;

    private Integer percentage;

    @Column(name="amount_sun")
    private Long amountSun;

    @Enumerated(EnumType.STRING)
    private ReferralCommissionStatus status = ReferralCommissionStatus.PENDING;
}
