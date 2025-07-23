package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.ipan.nrgyrent.itrx.AppConstants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@Table(name = "nrg_tariffs")
public class Tariff {
    private static final long ATODELEGATE_ADDITION = 400_000L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_tariffs_seq")
    @SequenceGenerator(name = "nrg_tariffs_seq", sequenceName = "nrg_tariffs_seq", allocationSize = 1)
    private Long id;

    @Column(name = "label")
    private String label;

    @Column(name = "tx_type_1_amount_sun")
    private Long transactionType1AmountSun = AppConstants.PRICE_65K; // default one, Just in case

    @Column(name = "tx_type_2_amount_sun")
    private Long transactionType2AmountSun = AppConstants.PRICE_131K; // default one, Just in case

    @Column(name = "is_predefined")
    private Boolean predefined = Boolean.FALSE;

    @Column(name = "is_active")
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getAutodelegateType1AmountSun() {
        return transactionType1AmountSun + ATODELEGATE_ADDITION;
    }

    public Long getAutodelegateType2AmountSun() {
        return transactionType2AmountSun + ATODELEGATE_ADDITION;
    }

    public Long getMaxAutodelegateFee() {
        return Math.max(
                getAutodelegateType1AmountSun(),
                getAutodelegateType2AmountSun()
        );
    }

}
