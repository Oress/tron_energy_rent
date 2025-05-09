package org.ipan.nrgyrent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class NrgyrentApplication {
    public static void main(String[] args) throws IOException {
        SpringApplication.run(NrgyrentApplication.class, args);
    }
}
