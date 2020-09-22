package sample

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("kafka")
data class KafkaProperties(
  val chunks: ChunksProperties,
  val marking: MarkingProperties,
  val txManager: TxManagerProperties
) {

//  val documentChunks = DocumentChunks()
//  val message = Message()

  data class DocumentChunks (
    val bootstrapServers : String,
    val topicProducer : String,
    val maxRequestSize : String
    )

  data class Message (
    val source: String,
    val formatVersion: String
  )

  data class ChunksProperties(
    val bootstrapServers: String,
    val groupId: String,
    val topic: ConsumerTopicProperties,
    val responseTimeout: Duration,
    val pollTimeout: Duration,
    val retryTimeout: Duration,
    val rollback: String,
    val max: ConsumerMaxProperties
  )

  data class TxManagerProperties(
    val bootstrapServers: String,
    val topic: ProducerTopicProperties,
    val max: ProducerMaxProperties
  )

  data class MarkingProperties(
    val topic: ProducerTopicProperties,
    val max: ProducerMaxProperties
  )

  data class ConsumerTopicProperties(
    val consumer: String
  )

  data class ProducerTopicProperties(
    val producer: String
  )

  data class ConsumerMaxProperties(
    val poll: PollProperties,
    val partitionFetchBytes: Double
  )

  data class ProducerMaxProperties(
    val requestSize: Double
  )

  data class PollProperties(
    val interval: Double,
    val records: Double
  )

}