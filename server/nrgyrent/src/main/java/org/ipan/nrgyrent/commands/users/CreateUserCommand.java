package org.ipan.nrgyrent.commands.users;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateUserCommand {
    private Long telegramId;
}
