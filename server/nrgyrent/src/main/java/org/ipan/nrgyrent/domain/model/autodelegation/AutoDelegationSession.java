package org.ipan.nrgyrent.domain.model.autodelegation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.ipan.nrgyrent.domain.model.AppUser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "nrg_autodelegation_sessions")
public class AutoDelegationSession {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_autodelegation_sessions_seq")
    @SequenceGenerator(name = "nrg_autodelegation_sessions_seq", sequenceName = "nrg_autodelegation_sessions_seq", allocationSize = 1)
    private Long id;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<AutoDelegationEvent> events = new ArrayList<>();

    @Column
    private String address;

    @Column(name = "message_to_update")
    private Integer messageToUpdate;

    @Column(name = "chat_id")
    private Long chatId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column
    @Enumerated(EnumType.STRING)
    private AutoDelegationSessionStatus status = AutoDelegationSessionStatus.ACTIVE;

    @Column(name = "last_sc_invocation_ts")
    private Long lastSmartContractTs;

    @Column(name = "is_active")
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
