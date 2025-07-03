package org.ipan.nrgyrent.utils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@AllArgsConstructor
public class Main implements CommandLineRunner {
    private final BybitRestClient bybitRestClient;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Start {}", String.join(" ", args));
        if (args.length == 1) {
            String command = args[0];
            if (command.equals("showintertrans")) {
                bybitRestClient.getInteralTransferHistory();
            }
        }
    }
}