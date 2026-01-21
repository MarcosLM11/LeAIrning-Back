package com.marcos.usersservice.config;

import com.marcos.usersservice.event.NotificationEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @PostConstruct
    void warmUpProducer() {
        var props = kafkaTemplate.getProducerFactory().getConfigurationProperties();
        var servers = props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
        log.debug("Warming up Kafka producer for bootstrap servers: {}", servers);
        kafkaTemplate.getProducerFactory().createProducer();
    }
}
