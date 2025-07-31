package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

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
    @ManyToOne(fetch = FetchType.LAZY)
    private Balance balance;

    @JoinColumn(name = "ref_prog_id")
    @ManyToOne
    private ReferralProgram referralProgram;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
