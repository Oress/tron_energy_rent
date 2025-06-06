package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "nrg_balance_referral_programs")
public class BalanceReferralProgram {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_balance_referral_programs_seq")
    @SequenceGenerator(name = "nrg_balance_referral_programs_seq", sequenceName = "nrg_balance_referral_programs_seq", allocationSize = 1)
    private Long id;

    @Column
    private String link;

    @JoinColumn(name = "balance_id")
    @ManyToOne
    private Balance balance;

    @JoinColumn(name = "ref_prog_id")
    @ManyToOne
    private ReferralProgram referralProgram;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
