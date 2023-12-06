package org.example.consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OtherConsumerTest {

    //多线程+多kafkaConsumer实例
    public static void main(String[] args) {

        String brokerList = "172.17.8.146:9092";
        String groupId = "group2";
        String topic = "www_topic";
        final int cpuCount = Runtime.getRuntime().availableProcessors();

        /**
         * Partition1
         * /--------------------------/
         * |   w1    |   w2   |  w3  |
         * /-------------------------/
         */
        ConsumerHandler consumers = new ConsumerHandler(brokerList, groupId, topic);
        consumers.consume(cpuCount);
//        try {
//            Thread.sleep(1000000);
//        } catch (InterruptedException ignored) {}
        consumers.shutdown();
    }
}

class Worker implements Runnable {

    private final Map<TopicPartition, OffsetAndMetadata> offsets;
    private ConsumerRecords<String, String> records;

    public Worker(ConsumerRecords records, Map<TopicPartition, OffsetAndMetadata> offsets) {
        this.records = records;
        this.offsets = offsets;
    }

    @Override
    public void run() {
        for (TopicPartition partition : records.partitions()){
            List<ConsumerRecord<String, String>> partitionRecords  = records.records(partition);
            for (ConsumerRecord record : partitionRecords){
                // 这里写你的消息处理逻辑，本例中只是简单地打印消息
                System.out.println(Thread.currentThread().getName() + " consumed " + record.partition()
                        + "th message with offset: " + record.offset());
            }

            long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
            synchronized (offsets){
                if (!offsets.containsKey(partition)){
                    offsets.put(partition, new OffsetAndMetadata(lastOffset+1));
                }else {
                    long curr = offsets.get(partition).offset();
                    if (curr <= lastOffset + 1){
                        offsets.replace(partition, new OffsetAndMetadata(lastOffset + 1));
                    }
                }
            }
        }

    }
}

class ConsumerHandler {

    // 本例中使用一个consumer将消息放入后端队列，你当然可以使用前一种方法中的多实例按照某张规则同时把消息放入后端队列
    private final KafkaConsumer<String, String> consumer;
    private ExecutorService executors;

    private final static Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();

    public ConsumerHandler(String brokerList, String groupId, String topic) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        List<String> interceptors = new ArrayList<>();
        interceptors.add("org.example.interceptor.AvgLatencyConsumerInterceptor");
        properties.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);

        this.consumer = new KafkaConsumer<>(properties);

        //这是干嘛
        consumer.subscribe(Arrays.asList(topic), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                consumer.commitSync(offsets);
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                offsets.clear();
            }
        });
    }

    public void consume(int workerNum) {
        executors = new ThreadPoolExecutor(workerNum, workerNum, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1000), new ThreadPoolExecutor.CallerRunsPolicy());

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(200);
                if (!records.isEmpty()){
                    executors.submit(new Worker(records, offsets));
                }
                commitOffsets();
            }
        } catch (WakeupException e) {
            // swallow this exception
        } finally {
            commitOffsets();
            consumer.close();
        }
    }

    private void commitOffsets(){
        // 尽量降低synchronized块对offsets锁定的时间
        Map<TopicPartition, OffsetAndMetadata> unmodfiedMap;
        synchronized (offsets){
            if (offsets.isEmpty()){
                return;
            }
            unmodfiedMap = Collections.unmodifiableMap(new HashMap<>(offsets));
            offsets.clear();
        }
        consumer.commitSync(unmodfiedMap);
    }

//    @Deprecated
//    public synchronized static void incrOffset(ConsumerRecord record){
//        offsets.put(new TopicPartition(record.topic(), record.partition()),
//                new OffsetAndMetadata(record.offset() + 1));
//    }

    public void close() {
        consumer.wakeup();
        executors.shutdown();
    }

    public void shutdown() {
        if (consumer != null) {
            consumer.close();
        }
        if (executors != null) {
            executors.shutdown();
        }
        try {
            if (!executors.awaitTermination(10, TimeUnit.SECONDS)) {
                System.out.println("Timeout.... Ignore for this case");
            }
        } catch (InterruptedException ignored) {
            System.out.println("Other thread interrupted this shutdown, ignore for this case.");
            Thread.currentThread().interrupt();
        }
    }

}