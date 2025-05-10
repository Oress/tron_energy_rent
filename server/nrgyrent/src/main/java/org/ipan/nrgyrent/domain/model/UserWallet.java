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
@Table(name = "nrg_user_wallets")
public class UserWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ngr_user_wallet_seq")
    @SequenceGenerator(name = "ngr_user_wallet_seq", sequenceName = "ngr_user_wallet_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "telegram_id", foreignKey = @ForeignKey(name = "fk_nrg_user_wallets_user_id"))
    private AppUser user;

    private String address;

    @CreationTimestamp
    private Instant createdAt;
}
