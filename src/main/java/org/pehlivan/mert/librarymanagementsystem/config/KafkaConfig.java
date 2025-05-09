package org.pehlivan.mert.librarymanagementsystem.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserRegistrationNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id:notification-group}")
    private String groupId;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic userRegistrationTopic() {
        return new NewTopic("user-registration", 1, (short) 1);
    }

    @Bean
    public ProducerFactory<String, UserRegistrationNotification> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(JsonSerializer.TYPE_MAPPINGS, "userRegistration:org.pehlivan.mert.librarymanagementsystem.dto.user.UserRegistrationNotification");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, UserRegistrationNotification> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, UserRegistrationNotification> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // 30 seconds
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000); // 10 seconds
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "org.pehlivan.mert.librarymanagementsystem.dto.user.UserRegistrationNotification");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, true);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserRegistrationNotification> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserRegistrationNotification> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setSyncCommits(true);
        factory.getContainerProperties().setConsumerStartTimeout(Duration.ofSeconds(30));
        factory.getContainerProperties().setPollTimeout(5000); // 5 seconds
        return factory;
    }
} 