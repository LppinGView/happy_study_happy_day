package org.example.interceptor;

import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.example.config.ZookeeperConfig;

import java.util.Map;

public class AvgLatencyConsumerInterceptor implements ConsumerInterceptor<String, String> {
    @Override
    public ConsumerRecords<String, String> onConsume(ConsumerRecords<String, String> records) {
//        long lantency = 0L;
//        for (ConsumerRecord<String, String> record : records) {
//            lantency += (System.currentTimeMillis() - record.timestamp());
//        }
//        ZookeeperConfig zookeeper = ZookeeperConfig.connet();
//        long beforeTotalLatency = zookeeper.getNodeData("/totalLatency");
//        if (beforeTotalLatency > 0){
//            lantency = beforeTotalLatency + lantency;
//        }
//        zookeeper.setNodeData("/totalLatency", lantency);
//        long totalLatency = zookeeper.getNodeData("/totalLatency");
//        long totalSentMsgs = zookeeper.getNodeData("/totalSentMessage");
//        zookeeper.setNodeData("/avgLatency", totalLatency / totalSentMsgs);
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {

    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
