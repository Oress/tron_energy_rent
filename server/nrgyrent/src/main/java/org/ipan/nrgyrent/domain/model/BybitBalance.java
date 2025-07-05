package org.ipan.nrgyrent.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "nrg_bybit_balance")
public class BybitBalance {
    @Id
    private String coin;

    private BigDecimal balance;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;
}
