package org.ipan.nrgyrent.cron;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.ipan.nrgyrent.itrx.AppConstants;

@Data
@AllArgsConstructor
public class ItrxAlertConfig {
    private String balanceId;
    private String alertName;

    public boolean isItrx() {
        return AppConstants.ITRX.equals(balanceId);
    }
}
