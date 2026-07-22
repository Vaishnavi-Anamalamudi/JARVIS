package com.adaptivegateway.kafka.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.adaptivegateway.common.exception.BusinessException;
import com.adaptivegateway.common.exception.ErrorCode;
import com.adaptivegateway.config.ApplicationProperties;
import com.adaptivegateway.kafka.config.KafkaIntegrationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class KafkaHealthServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaHealthService service;

    @BeforeEach
    void setUp() {
        service = new KafkaHealthService(
                kafkaTemplate,
                properties(),
                new ApplicationProperties("adaptive-api-gateway", "v1"),
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void readHealthPublishesProbeEvent() {
        when(kafkaTemplate.send(eq("gateway.health"), any(String.class), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult()));

        var response = service.readHealth("0d13751b-2249-46ea-8ef9-d77e3b89aaf8");

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.healthTopic()).isEqualTo("gateway.health");
    }

    @Test
    void readHealthMapsSendFailureToBusinessException() {
        when(kafkaTemplate.send(eq("gateway.health"), any(String.class), any(String.class)))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("broker unavailable")));

        assertThatThrownBy(() -> service.readHealth("0d13751b-2249-46ea-8ef9-d77e3b89aaf8"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.KAFKA_UNAVAILABLE);
    }

    private static KafkaIntegrationProperties properties() {
        return new KafkaIntegrationProperties(
                "gateway.events",
                "gateway.requests",
                "gateway.health",
                1,
                1000,
                30,
                3,
                50,
                10000
        );
    }

    private static SendResult<String, String> sendResult() {
        return new SendResult<>(new ProducerRecord<>("gateway.health", "key", "value"), null);
    }
}
