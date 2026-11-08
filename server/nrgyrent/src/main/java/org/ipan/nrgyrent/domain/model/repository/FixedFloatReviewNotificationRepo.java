package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.FixedFloatReviewNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedFloatReviewNotificationRepo
        extends JpaRepository<FixedFloatReviewNotification, FixedFloatReviewNotification.Id> {
}
