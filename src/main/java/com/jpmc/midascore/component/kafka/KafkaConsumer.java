package com.jpmc.midascore.component.kafka;

import com.jpmc.midascore.foundation.Transaction;

import java.util.logging.Logger;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {
 
    @KafkaListener(
            topics = "${general.kafka-topic}",
            groupId = "midas-core-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(Transaction transaction) {
        // Intentionally empty for this task -- receiving/deserializing
        // is the goal; handling the transaction comes in a later task.
        Logger.getLogger(KafkaConsumer.class.getName()).log(java.util.logging.Level.INFO, "Received transaction: {0}", transaction);
    }
}