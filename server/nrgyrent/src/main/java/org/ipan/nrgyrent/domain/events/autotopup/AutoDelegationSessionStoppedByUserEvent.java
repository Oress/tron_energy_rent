package org.ipan.nrgyrent.domain.events.autotopup;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

@Getter
@ToString
public class AutoDelegationSessionStoppedByUserEvent extends ApplicationEvent {
    private final Long sessionId;

    public AutoDelegationSessionStoppedByUserEvent(Object source, Long sessionId) {
        super(source);
        this.sessionId = sessionId;
    }
}
