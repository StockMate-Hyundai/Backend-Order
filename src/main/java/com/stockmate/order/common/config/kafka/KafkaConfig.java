package com.stockmate.order.common.config.kafka;

import com.stockmate.order.api.order.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper;
import org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Producer Configuration
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Producer가 메시지 헤더에 타입 ID를 추가하도록 설정
        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);

        Map<String, Class<?>> classIdMapping = new HashMap<>();
        classIdMapping.put("receivingHistoryRequest", ReceivingHistoryRequestEvent.class);
        typeMapper.setIdClassMapping(classIdMapping);
        jsonSerializer.setTypeMapper(typeMapper);

        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        log.info("KafkaTemplate 생성 (Type Mapping 적용)");
        return new KafkaTemplate<>(producerFactory());
    }

    // Consumer Configuration
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Consumer용 타입 매핑 설정
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        
        Map<String, Class<?>> classIdMapping = new HashMap<>();
        classIdMapping.put("receivingHistorySuccess", ReceivingHistorySuccessEvent.class);
        classIdMapping.put("receivingHistoryFailed", ReceivingHistoryFailedEvent.class);
        typeMapper.setIdClassMapping(classIdMapping);
        jsonDeserializer.setTypeMapper(typeMapper);
        jsonDeserializer.addTrustedPackages("com.stockmate.order.api.order.dto");

        log.info("Kafka Consumer Factory 설정 완료 - Bootstrap Servers: {}", bootstrapServers);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new ErrorHandlingDeserializer<>(jsonDeserializer));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);

        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
                (record, exception) -> {
                    log.error("Kafka 메시지 처리 실패 - 토픽: {}, 파티션: {}, 오프셋: {}, 에러: {}",
                            record.topic(), record.partition(), record.offset(), exception.getMessage());
                },
                new org.springframework.util.backoff.FixedBackOff(1000L, 3)
        ));

        log.info("Kafka Listener Container Factory 설정 완료");
        return factory;
    }
}
