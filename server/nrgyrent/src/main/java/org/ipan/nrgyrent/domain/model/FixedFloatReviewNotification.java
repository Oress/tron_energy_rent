package org.ipan.nrgyrent.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "nrg_ff_review_notifications")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FixedFloatReviewNotification {
    @EmbeddedId
    private Id id;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "deposit_id")
        private Long depositId;

        @Column(name = "telegram_id")
        private Long telegramId;
    }
}
