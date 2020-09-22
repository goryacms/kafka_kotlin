package sample

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.stereotype.Component
import sample.AtLeastOnceProcessor
import sample.KafkaProperties

@Component
class DocumentReceiver(
  consumer: Consumer<String, String>,
  kafkaProperties: KafkaProperties
): AtLeastOnceProcessor<String, String>(
  consumer,
  listOf(kafkaProperties.chunks.topic.consumer),
  kafkaProperties.chunks.pollTimeout,
  respawnTimeout = kafkaProperties.chunks.retryTimeout
) {

  override suspend fun processing(records: ConsumerRecords<String, String>) {
    records.forEach { consumerRecord ->
      println("consumer record = " + consumerRecord.value())
    }
  }

}