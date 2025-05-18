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
@Table(name = "nrg_manual_balance_changes")
public class ManualBalanceAdjustmentAction {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_manual_balance_changes_seq")
    @SequenceGenerator(name = "nrg_manual_balance_changes_seq", sequenceName = "nrg_manual_balance_changes_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "balance_id")
    private Balance balance;

    @ManyToOne
    @JoinColumn(name = "changed_by")
    private AppUser changedBy;

    @Column(name = "amount_from")
    private Long amountFrom;

    @Column(name = "amount_to")
    private Long amountTo;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
