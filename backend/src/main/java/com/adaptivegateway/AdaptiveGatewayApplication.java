package com.adaptivegateway;

import com.adaptivegateway.adaptive.config.AdaptiveLearningProperties;
import com.adaptivegateway.anomaly.config.AnomalyDetectionProperties;
import com.adaptivegateway.config.ApplicationProperties;
import com.adaptivegateway.kafka.config.KafkaIntegrationProperties;
import com.adaptivegateway.redis.config.RedisIntegrationProperties;
import com.adaptivegateway.security.AuthSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        ApplicationProperties.class,
        AuthSecurityProperties.class,
        RedisIntegrationProperties.class,
        KafkaIntegrationProperties.class,
        AdaptiveLearningProperties.class,
        AnomalyDetectionProperties.class
})
public class AdaptiveGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdaptiveGatewayApplication.class, args);
    }
}
