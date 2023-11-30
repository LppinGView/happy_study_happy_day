package org.example.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.config.ZookeeperConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ProducerTest {
    private static final Logger log = LoggerFactory.getLogger(ProducerTest.class);


    public void pushMsg() throws IOException {
        ZookeeperConfig zookeeper = ZookeeperConfig.connet();
        log.info("zookeeper >>> {}", zookeeper);
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "172.17.8.146:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
//        properties.put(ProducerConfig.ACKS_CONFIG, "all");
//        properties.put(ProducerConfig.RETRIES_CONFIG, "3");
//        properties.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, "3");
        // 开启GZIP压缩
        properties.put("compression.type", "gzip");

        List<String> interceptors = new ArrayList<>();
        interceptors.add("org.example.interceptor.AvgLatencyProducerInterceptor");
        properties.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);

        Producer<String, String> producer = new KafkaProducer<>(properties);
//        properties.put(ProducerConfig.)
        log.info("发送消息");

        ProducerRecord<String, String> record = new ProducerRecord<>("my_topic_zip","Hello, Kafka!");

//        producer.partitionsFor();
        for (;;){
//            producer.send(record);
            producer.send(record, (a, b)->{
                //更新发送端 的状态
                System.out.println("i am ok");
            });
            log.info("消息已发送， 消息为:{}", record);
        }

//        producer.close();
    }

    public static void main(String[] args) throws IOException {
        ProducerTest test = new ProducerTest();
        test.pushMsg();
    }
}
