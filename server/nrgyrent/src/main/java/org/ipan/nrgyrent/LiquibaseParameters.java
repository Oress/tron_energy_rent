package org.ipan.nrgyrent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "spring.liquibase.parameters")
public class LiquibaseParameters {
    private Long defaultRefProgramId;
    private String defaultRefProgramLabel;
    private Integer defaultRefProgramPercentage;
    private Long defaultBalanceDailyLimit;
}