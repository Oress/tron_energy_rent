package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "nrg_itrx_api_summary_stats")
public class ItrxApiStats {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_itrx_api_summary_stats_seq")
    @SequenceGenerator(name = "nrg_itrx_api_summary_stats_seq", sequenceName = "nrg_itrx_api_summary_stats_seq", allocationSize = 1)
    private Long id;

    @Column(name = "balance_sun")
    private Long balanceSun;

    @Column(name = "total_orders_count")
    private Integer totalOrdersCount;

    @Column(name = "total_sum_energy")
    private Long totalSumEnergy;

    @Column(name = "total_sum_trx")
    private Long totalSumTrx;

    @CreationTimestamp
    private Instant timestamp;
}
