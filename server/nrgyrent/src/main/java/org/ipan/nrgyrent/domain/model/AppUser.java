package org.ipan.nrgyrent.domain.model;

import java.math.BigDecimal;
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

    @Column(name = "deposit_address")
    private String depositAddress;

    @Column(name = "trx_balance", precision = 19, scale = 6)
    private BigDecimal trxBalance = new BigDecimal(0);

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    private boolean disabled;

    @Column(name = "disabled_reason")
    private String disabledReason;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;


}
