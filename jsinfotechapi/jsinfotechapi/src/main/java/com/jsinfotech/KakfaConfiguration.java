package com.jsinfotech;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.jsinfotech.Domain.Gate2;
import com.jsinfotech.Domain.Gate;
import com.jsinfotech.Domain.Gate1;

@Configuration
public class KakfaConfiguration {
	
	 	@Value(value = "${kafka.bootstrapAddress}")
	    private String bootstrapAddress;
	 
	    @Value(value = "${general.topic.group.id}")
	    private String groupId;
	 
	    @Value(value = "${user.topic.group.id}")
	    private String userGroupId;

    @Bean
    public ProducerFactory<String, Gate> producerFactory() {
        Map<String, Object> config = new HashMap();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<String, Gate>(config);
    }


    @Bean
    public KafkaTemplate<String, Gate> kafkaTemplate() {
        return new KafkaTemplate<String, Gate>(producerFactory());
    }
    
    @Bean
    public ProducerFactory<String, Gate1> producerFactory1() {
        Map<String, Object> config = new HashMap();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<String, Gate1>(config);
    }

    @Bean
    public ProducerFactory<String, Gate2> producerFactory2() {
        Map<String, Object> config = new HashMap();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<String, Gate2>(config);
    }

    @Bean
    public KafkaTemplate<String, Gate1> kafkaTemplate1() {
        return new KafkaTemplate<String, Gate1>(producerFactory1());
    }

    @Bean
    public KafkaTemplate<String, Gate2> kafkaTemplate2() {
        return new KafkaTemplate<String, Gate2>(producerFactory2());
    }
    
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
                StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
                StringDeserializer.class);
        //props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<String, String>(props);
    }
 
    @Bean
    public ConsumerFactory<String, String> consumerFactory1() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
                StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
                StringDeserializer.class);
        //props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<String, String>(props);
    }
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> 
                        kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory 
            = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

}
