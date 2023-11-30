package org.example.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class ConsumerTest {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "172.17.8.146:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "1");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        List<String> interceptors = new ArrayList<>();
        interceptors.add("org.example.interceptor.AvgLatencyConsumerInterceptor");
        properties.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);

        Consumer<String, String> consumer = new KafkaConsumer<>(properties);
//        consumer.assign(Collections.singletonList(new TopicPartition("my_topic_zip", 0)));
        consumer.subscribe(Collections.singletonList("my_topic_zip"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            consumer.commitSync();
            records.forEach(record -> {
                System.out.printf("Consumed record with key %s and value %s%n and partition %s%n", record.key(), record.value(), record.partition());
            });
        }
    }
}
