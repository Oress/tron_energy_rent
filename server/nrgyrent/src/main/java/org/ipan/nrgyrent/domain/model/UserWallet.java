package org.ipan.nrgyrent.domain.model;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
@ToString
public class UserWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_wallet_seq")
    @SequenceGenerator(name = "user_wallet_seq", sequenceName = "user_wallet_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "telegramId")
    private AppUser user;

    private String address;

    @CreationTimestamp
    private Instant createdAt;
}
