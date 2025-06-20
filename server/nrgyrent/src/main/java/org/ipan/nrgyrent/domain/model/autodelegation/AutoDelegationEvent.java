package org.ipan.nrgyrent.domain.model.autodelegation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.ipan.nrgyrent.domain.model.Order;

@Entity
@Getter
@Setter
@Table(name = "nrg_autodelegation_events")
public class AutoDelegationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_autodelegation_events_seq")
    @SequenceGenerator(name = "nrg_autodelegation_events_seq", sequenceName = "nrg_autodelegation_events_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private AutoDelegationSession session;

    @Column
    @Enumerated(EnumType.STRING)
    private AutoDelegationEventType type;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Column
    private Long timestamp;
}
