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
@Table(name = "nrg_configs")
public class NrgConfigs {
    @Id
    private String id;

    private String value;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;
}
