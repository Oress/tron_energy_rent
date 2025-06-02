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
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@Setter
@Table(name = "nrg_users")
@Slf4j
public class AppUser {
    @Id
    @Column(name = "telegram_id")
    private Long telegramId;

    @Column(name = "telegram_username")
    private String telegramUsername;

    @Column(name = "telegram_first_name")
    private String telegramFirstName;

    @JoinColumn(name = "balance_id")
    @ManyToOne
    private Balance balance;

    @JoinColumn(name = "group_balance_id")
    @ManyToOne
    private Balance groupBalance;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    private Boolean disabled = Boolean.FALSE;

    @Column(name = "disabled_reason")
    private String disabledReason;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    // If group balance is present -> use it, otherwise use personal balance.
    public Balance getBalanceToUse() {
        Balance bal = groupBalance == null 
            ? balance 
            : groupBalance;

        if (bal == null) {
            logger.error("User {} has no bal {}", telegramUsername);
        }

        return bal;
    }

    // If group balance is present -> use it, otherwise use personal balance.
    public Tariff getTariffToUse() {
        Balance bal = getBalanceToUse();

        if (bal == null || bal.getTariff() == null) {
            logger.error("User {} has no tariff set bal", telegramUsername);
        }

        return bal.getTariff();
    }

    public Boolean isInGroup() {
        return groupBalance != null;
    }

    public Boolean isGroupManager() {
        boolean result = false;

        if (groupBalance != null) {
            AppUser manager = groupBalance.getManager();
            result = manager != null ? telegramId.equals(manager.getTelegramId()) : false;
        }
        
        return result;
    }
}
