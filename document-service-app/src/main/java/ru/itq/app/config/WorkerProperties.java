package ru.itq.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.worker")
public class WorkerProperties {

    private int batchSize = 50;
    private long submitDelay = 30000;
    private long approveDelay = 30000;
}
