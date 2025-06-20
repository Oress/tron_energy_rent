package org.ipan.nrgyrent.application.events;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

@Getter
@ToString
public class FullNodeDisconnectedEvent extends ApplicationEvent {

    public FullNodeDisconnectedEvent(Object source) {
        super(source);
    }
}
