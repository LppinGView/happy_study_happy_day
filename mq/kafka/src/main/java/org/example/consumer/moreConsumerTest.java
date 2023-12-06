package org.example.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class moreConsumerTest {
    public static void main(String[] args) {
        // 由于消息分区只有4个,且已经分配完了,这里的消费者无法获取多余的分区
        // 这里当出现一个线程 阻塞时, 出现消息处理速度慢时,可能会出现Rebalance.从而引发整个消费组的消费停滞.
        new Thread(() -> {
            makeConsumer("5");
        }).start();

        new Thread(() -> {
            makeConsumer("6");
        }).start();
    }

    public static void makeConsumer(String role){
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "172.17.8.146:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "1");
        properties.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, role);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        List<String> interceptors = new ArrayList<>();
        interceptors.add("org.example.interceptor.AvgLatencyConsumerInterceptor");
        properties.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);


        Consumer<String, String> consumer = new KafkaConsumer<>(properties);
//        consumer.assign(Collections.singletonList(new TopicPartition("my_topic_zip", 0)));
        consumer.subscribe(Collections.singletonList("www_topic"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            consumer.commitSync();
            records.forEach(record -> {
                System.out.printf("Consumed" + role + " record with key %s and value %s%n and partition %s%n", record.key(), record.value(), record.partition());
            });
        }
    }
}
