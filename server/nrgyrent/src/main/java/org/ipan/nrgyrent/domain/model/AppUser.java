package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
@Table(name = "nrg_users")
public class AppUser {
    @Id
    @Column(name = "telegram_id")
    private Long telegramId;

//    @OneToOne
//    private DepositWallet depositWallet;

    private String role;

    private boolean disabled;

    @Column(name = "disabled_reason")
    private String disabledReason;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
