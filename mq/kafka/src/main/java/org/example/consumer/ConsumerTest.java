package org.example.consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;

public class ConsumerTest {
    public static void main(String[] args) {

        String brokerList = "172.17.8.146:9092";
        String groupId = "testGroup1";
        String topic = "www_topic";
        int consumerNum = 7;

        /**
         * Partition1
         * /--------------------------/
         * |            c1           |
         * /-------------------------/
         */
        ConsumerGroup consumerGroup = new ConsumerGroup(consumerNum, groupId, topic, brokerList);
        consumerGroup.execute();
    }
}

class ConsumerGroup {
    private List<ConsumerRunnable> consumers;

    public ConsumerGroup(int consumerNum, String groupId, String topic, String brokerList) {
        consumers = new ArrayList<>(consumerNum);
        for (int i = 0; i < consumerNum; ++i) {
            ConsumerRunnable consumerThread = new ConsumerRunnable(brokerList, groupId, topic);
            consumers.add(consumerThread);
        }
    }

    public void execute() {
        for (ConsumerRunnable task : consumers) {
            new Thread(task).start();
        }
    }
}

class ConsumerRunnable implements Runnable {

    private static Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
    // 每个线程维护私有的KafkaConsumer实例
    private final KafkaConsumer<String, String> consumer;

    public ConsumerRunnable(String brokerList, String groupId, String topic) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 不自动提交

        List<String> interceptors = new ArrayList<>();
        interceptors.add("org.example.interceptor.AvgLatencyConsumerInterceptor");
        properties.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);

        this.consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singletonList(topic));
    }

    @Override
    public void run() {
//        processAndCommit();
        processAndBatchCommit(100);
    }

    public void processAndBatchCommit(int batchSize){
        int count = 0;
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<String, String> record: records){
                processMsg(record);
                offsets.put(new TopicPartition(record.topic(), record.partition()),
                        new OffsetAndMetadata(record.offset() + 1));

                //100个一批 提交
                if(count % batchSize == 0){
                    System.out.println("==> 100个一批,统一提交");
                    consumer.commitAsync(offsets, null); // 回调处理逻辑是null
                }
                count++;
            }
        }
    }

    private void processAndCommit(){
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(200);   // 本例使用200ms作为获取超时时间
            for (ConsumerRecord<String, String> record : records) {
                processMsg(record);
            }
            consumer.commitSync();//同步提交
        }
    }

    private void processMsg(ConsumerRecord<String, String> record){
        // 这里面写处理消息的逻辑，本例中只是简单地打印消息
        System.out.println(Thread.currentThread().getName() + " consumed " + record.partition() +
                "th message with offset: " + record.offset());
    }
}
