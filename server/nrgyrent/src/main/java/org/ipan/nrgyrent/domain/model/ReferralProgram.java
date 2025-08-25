package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.ipan.nrgyrent.itrx.AppConstants;

@Entity
@Getter
@Setter
@Table(name = "nrg_referral_programs")
public class ReferralProgram {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_referral_programs_seq")
    @SequenceGenerator(name = "nrg_referral_programs_seq", sequenceName = "nrg_referral_programs_seq", allocationSize = 1)
    private Long id;

    @Column
    private String label;

    @Column
    private Integer percentage;

    // ITRX provider
    @Column(name = "subtract_amount")
    private Long subtractAmountTx1Itrx;
    @Column(name = "subtract_amount_tx2")
    private Long subtractAmountTx2Itrx;

    // ITRX auto delegation
    @Column(name = "subtract_amount_tx1_auto_itrx")
    private Long subtractAmountTx1AutoItrx;
    @Column(name = "subtract_amount_tx2_auto_itrx")
    private Long subtractAmountTx2AutoItrx;

    // CATFEE provider
    @Column(name = "subtract_amount_tx1_catfee")
    private Long subtractAmountTx1Catfee;

    @Column(name = "subtract_amount_tx2_catfee")
    private Long subtractAmountTx2Catfee;

    @Column(name="calc_type")
    @Enumerated(EnumType.STRING)
    private ReferralProgramCalcType calcType = ReferralProgramCalcType.PERCENT_FROM_PROFIT;

    @Column(name = "is_predefined")
    private Boolean predefined = Boolean.FALSE;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
