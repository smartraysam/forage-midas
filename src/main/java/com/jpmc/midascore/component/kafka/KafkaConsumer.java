package com.jpmc.midascore.component.kafka;

import com.jpmc.midascore.component.DatabaseConduit;
import com.jpmc.midascore.foundation.Transaction;

import java.util.logging.Logger;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class KafkaConsumer {
 
    private final DatabaseConduit databaseConduit;
 
    public KafkaConsumer(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
    }
 
    @KafkaListener(
            topics = "${general.kafka-topic}",
            groupId = "midas-core-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(Transaction transaction) {
        Logger.getLogger(KafkaConsumer.class.getName()).info("Received transaction: " + transaction);
        databaseConduit.processTransaction(transaction);
    }
}
 