package org.ipan.nrgyrent.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "nrg_itrx_balance")
public class ItrxBalance {
    @Id
    private String id;

    private Long balance;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;
}
