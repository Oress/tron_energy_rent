package org.ipan.nrgyrent.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@ToString
@Table(name = "nrg_orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_order_seq")
    @SequenceGenerator(name = "nrg_order_seq", sequenceName = "nrg_order_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "telegram_id", foreignKey = @ForeignKey(name = "fk_nrg_orders_user_id"))
    private AppUser user;

    @Column(name = "itrx_fee_sun_amount")
    private Long itrxFeeSunAmount;

    @Column(name = "sun_amount")
    private Long sunAmount;

    @Column(name = "energy_amount")
    private Integer energyAmount;

    @Column(name = "receive_address")
    private String receiveAddress;

    private String duration;

    @Column(name = "correlation_id")
    private String correlationId;

    private String serial;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;

    // Result from the callback
    @Column(name = "itrx_status")
    private Integer itrxStatus;

    @Column(name = "tx_id")
    private String txId;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
