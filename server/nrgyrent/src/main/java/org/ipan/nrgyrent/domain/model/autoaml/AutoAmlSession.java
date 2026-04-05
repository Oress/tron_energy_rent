package org.ipan.nrgyrent.domain.model.autoaml;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.ipan.nrgyrent.domain.model.AppUser;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "nrg_auto_aml_sessions")
public class AutoAmlSession {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrg_auto_aml_sessions_seq")
    @SequenceGenerator(name = "nrg_auto_aml_sessions_seq", sequenceName = "nrg_auto_aml_sessions_seq", allocationSize = 1)
    private Long id;

    @Column
    private String address;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "threshold_sun")
    private Long thresholdSun;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "message_to_update")
    private Integer messageToUpdate;

    @Column(name = "is_active")
    private Boolean active = Boolean.TRUE;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "deactivation_reason")
    private AutoAmlSessionDeactivationReason deactivationReason;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
