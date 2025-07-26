package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
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
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;

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

    @ManyToOne
    @JoinColumn(name = "balance_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_nrg_orders_balance_id"))
    private Balance balance;

    @ManyToOne
    @JoinColumn(name = "tariff_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_nrg_orders_tariff_id"))
    private Tariff tariff;

    @ManyToOne
    @JoinColumn(name = "autodelegate_session_id")
    private AutoDelegationSession autoDelegationSession;

    @Column(name = "itrx_fee_sun_amount")
    private Long itrxFeeSunAmount;

    @Column(name = "sun_amount")
    private Long sunAmount;

//    This field contains the remainder refProgram. (i.e. instead of actual itrx.io commission it uses nrg_referral_programs)
    @Column(name = "ref_program_profit_remainder")
    private Long refProgramProfitRemainder;

    @Column(name = "tx_amount")
    private Integer txAmount;

    @Column(name = "energy_amount")
    private Integer energyAmount;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "message_to_update")
    private Integer messageToUpdate;

    @Column(name = "receive_address")
    private String receiveAddress;

    private String duration;

    @Column(name = "correlation_id")
    private String correlationId;

    private String serial;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    private OrderType type;

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
