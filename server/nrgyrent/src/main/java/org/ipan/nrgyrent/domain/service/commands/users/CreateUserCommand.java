package org.ipan.nrgyrent.domain.service.commands.users;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateUserCommand {
    private Long telegramId;
    private String username;
    private String firstName;
}
