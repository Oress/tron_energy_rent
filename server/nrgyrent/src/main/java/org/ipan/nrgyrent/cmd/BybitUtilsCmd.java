package org.ipan.nrgyrent.cmd;

import lombok.AllArgsConstructor;
import org.ipan.nrgyrent.bybit.BybitRestClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class BybitUtilsCmd implements CommandLineRunner {
    private final BybitRestClient bybitRestClient;

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 1) {
            String command = args[0];
            if (command.equals("showintertrans")) {
                bybitRestClient.getInteralTransferHistory();
            }
        }
    }
}
