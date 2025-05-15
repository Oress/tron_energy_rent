package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "nrg_users")
public class AppUser {
    @Id
    @Column(name = "telegram_id")
    private Long telegramId;

    @JoinColumn(name = "balance_id")
    @ManyToOne
    private Balance balance;

    @JoinColumn(name = "group_balance_id")
    @ManyToOne
    private Balance groupBalance;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    private boolean disabled;

    @Column(name = "disabled_reason")
    private String disabledReason;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
