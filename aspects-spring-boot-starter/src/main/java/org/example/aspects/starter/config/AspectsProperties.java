package org.example.aspects.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "aspects")
public class AspectsProperties {

    private Metric metric = new Metric();
    private Cache cache = new Cache();
    private Datasource datasource = new Datasource();
    private HttpRequest httpRequest = new HttpRequest();
    @Data
    public static class Metric {
        private boolean enabled = true;
        private boolean kafkaEnabled = true;
        private long executionTimeLimit = 3000;
        private String kafkaTopic = "service_logs";
    }

    @Data
    public static class Cache {
        private boolean enabled = true;
        private long defaultTtl = 300000;
        private int cleanupInterval = 30;
    }

    @Data
    public static class Datasource {
        private boolean enabled = true;
        private boolean kafkaEnabled = true;
        private boolean fallbackToDatabase = true;
        private String kafkaTopic = "service_logs";
    }

    @Data
    public static class HttpRequest {
        private boolean enabled = true;
        private boolean kafkaEnabled = true;
        private boolean fallbackToDatabase = true;
        private String kafkaTopic = "service_logs";
    }
}