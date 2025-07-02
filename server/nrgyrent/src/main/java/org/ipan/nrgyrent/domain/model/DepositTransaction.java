package org.ipan.nrgyrent.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "nrg_deposit_transactions")
public class DepositTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_deposit_transactions_seq")
    @SequenceGenerator(name = "nrg_deposit_transactions_seq", sequenceName = "nrg_deposit_transactions_seq", allocationSize = 1)
    private Long id;

    @Column(name = "wallet_to", nullable = false)
    private String walletTo;

    @Column(name = "wallet_from", nullable = false)
    private String walletFrom;

    @Column(nullable = false)
    private Long amount;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order systemOrder;

    @Column(name = "tx_id", nullable = false, unique = true)
    private String txId;

    @Enumerated(EnumType.STRING)
    private DepositType type;

    @Column(name = "original_amount")
    private Long originalAmount;

    // for usdt deposits, because it involves multiple steps
    @Enumerated(EnumType.STRING)
    private DepositStatus status;

    @Column(name = "usdt_to_trx_rate", precision = 20, scale = 6)
    private BigDecimal trxToUsdtRate;

    @Column(name = "activation_fee_sun") // 1.1 TRX in case of inactive wallet
    private Long activationFeeSun;

    @Column(name = "bybit_usdt_tx")
    private String bybitUsdtTx;

    @Column(name = "bybit_fee_sun") // 0.15 % ?
    private Long bybitFeeSun;

    @Column(name = "bybit_transfer_id")
    private String bybitTransferId;

    @Column(name = "bybit_order_id")
    private String bybitOrderId;

    @Column
    private Long timestamp;

    public boolean includeWalletActivationFee() {
        return DepositType.USDT.equals(type) && activationFeeSun != null && activationFeeSun > 0;
    }

    public Long getActivationFeeSun() {
        return activationFeeSun == null ? 0L : activationFeeSun;
    }
}
