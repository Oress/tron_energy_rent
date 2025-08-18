package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "nrg_balances")
public class Balance {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_balance_seq")
    @SequenceGenerator(name = "nrg_balance_seq", sequenceName = "nrg_balance_seq", allocationSize = 1)
    private Long id;

    @Column(name = "deposit_address")
    private String depositAddress;

    private String label;

    @JoinColumn(name = "bal_ref_prog_id")
    @ManyToOne
    private BalanceReferralProgram referralProgram;

    @JoinColumn(name = "manager_id")
    @OneToOne
    private AppUser manager;

    @JoinColumn(name = "tariff_id")
    @OneToOne
    private Tariff tariff;

    @Enumerated(EnumType.STRING)
    private BalanceType type = BalanceType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_provider")
    private EnergyProviderName energyProvider;

    @Column(name = "sun_balance")
    private Long sunBalance = 0L;

    @Column(name = "last_tx_id")
    private String lastTxId;

    @Column(name = "last_tx_timestamp")
    private Long lastTxTimestamp;

    @Column(name = "last_trc20_tx_id")
    private String lastTrc20TxId;

    @Column(name = "last_trc20_tx_timestamp")
    private Long lastTrc20TxTimestamp;

    @Column(name = "daily_withdrawal_limit_sun")
    private Long dailyWithdrawalLimitSun;

    @Column(name = "daily_withdrawal_remaining_sun")
    private Long dailyWithdrawalRemainingSun;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @Version
    private Long version;

    public boolean isGroup() {
        return BalanceType.GROUP.equals(type);
    }

    public Long makeDeposit(Long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        sunBalance += amount;
        return sunBalance;
    }

    public String getIdAndLabel() {
        return "ID: %s  Label: %s".formatted(id, label);
    }

    public boolean canWithdraw(Long amountSun) {
        return dailyWithdrawalRemainingSun >= amountSun;
    }

    public void resetDailyWithdrawalLimit() {
        dailyWithdrawalRemainingSun = dailyWithdrawalLimitSun;
    }
}
