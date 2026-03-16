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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@Table(name = "nrg_aml_verifications")
public class AmlVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_aml_verifications_seq")
    @SequenceGenerator(name = "nrg_aml_verifications_seq", sequenceName = "nrg_aml_verifications_seq", allocationSize = 1)
    private Long id;

    @Column(name = "client_order_id")
    private String clientOrderId;

    @JoinColumn(name = "balance_id")
    @ManyToOne
    private Balance balance;

    @JoinColumn(name = "tariff_id")
    @ManyToOne
    private Tariff tariff;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private AmlVerificationPaymentStatus paymentStatus;

    // Netts fee
    @Column(name = "fee_usdt")
    private Double feeUsdt;

    // Netts fee
    @Column(name = "fee_sun")
    private Long feeSun;

    // Our fee
    @Column(name = "paid_sun")
    private Long paidSun;

    // Netts currency
    @Column(name = "currency")
    private String currency;

    // Netts msg
    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "wallet_address")
    private String walletAddress;

    // Verificaiton data:
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AmlVerificationStatus status;

    @Column(name = "risk_score")
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private AmlRiskLevel riskLevel;

    @Column(name = "is_sanctioned")
    private Boolean sanctioned;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private AmlProvider provider;

    @Column(name = "result", columnDefinition = "text")
    private String result;
    // End of verificaiton data

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
