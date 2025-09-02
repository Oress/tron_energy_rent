package org.ipan.nrgyrent.domain.service;

import lombok.AllArgsConstructor;
import org.ipan.nrgyrent.domain.model.Alert;
import org.ipan.nrgyrent.domain.model.AlertStatus;
import org.ipan.nrgyrent.domain.model.repository.AlertRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@AllArgsConstructor
public class AlertService {
    private final AlertRepo alertRepo;

    @Transactional
    public Alert createAlert(Long triggerValue, String alertName) {
        Alert alert = new Alert();
        alert.setName(alertName);
        alert.setTriggerValue(triggerValue.toString());
        alertRepo.save(alert);

        return alert;
    }


    @Transactional
    public Alert createCatfeeBalanceLowAlert(Long triggerValue) {
        Alert alert = new Alert();
        alert.setName(Alert.CATFEE_BALANCE_LOW);
        alert.setTriggerValue(triggerValue.toString());
        alertRepo.save(alert);

        return alert;
    }

    @Transactional
    public Alert resolveBalanceLowAlert(Long alertId) {
        Alert alert = alertRepo.findById(alertId).orElseThrow(() -> new IllegalArgumentException("Alert not found with id: " + alertId));
        alert.setStatus(AlertStatus.CLOSED);
        alert.setResolvedAt(Instant.now());
        return alert;
    }

}
