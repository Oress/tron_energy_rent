package org.ipan.nrgyrent.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "nrg_alerts")
public class Alert {
    public static final String CATFEE_BALANCE_LOW = "catfee_balance_low";
    public static final String ITRX_BALANCE_LOW = "itrx_balance_low";
    public static final String TRXX_BALANCE_LOW = "trxx_balance_low";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_alerts_seq")
    @SequenceGenerator(name = "nrg_alerts_seq", sequenceName = "nrg_alerts_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "trigger_value")
    private String triggerValue;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreationTimestamp
    @Column(name = "triggered_at")
    private Instant triggeredAt;
}
