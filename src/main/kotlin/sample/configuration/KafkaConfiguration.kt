package sample.configuration

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import sample.KafkaProperties
import java.net.InetAddress

@Configuration
class KafkaConfiguration {

  private val hostName: String
    get() {
      return InetAddress.getLocalHost().hostName
    }

  @ExperimentalStdlibApi
  @Bean
  fun kafkaConsumer(
//    appProperties: AppProperties,
    kafkaProperties: KafkaProperties
  ): KafkaConsumer<String, String> = buildMap<String, Any> {
    this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] =
      kafkaProperties.chunks.bootstrapServers
    this[CommonClientConfigs.CLIENT_ID_CONFIG] = "${kafkaProperties.chunks.groupId}.${hostName}"
    this[ConsumerConfig.GROUP_ID_CONFIG] = kafkaProperties.chunks.groupId
    this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
    this[ConsumerConfig.ISOLATION_LEVEL_CONFIG] = "read_committed"
    this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
    this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] =
      kafkaProperties.chunks.max.poll.records.toInt()
    this[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] =
      kafkaProperties.chunks.max.poll.interval.toInt()
    this[ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG] =
      kafkaProperties.chunks.max.partitionFetchBytes.toInt()
    this[ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG] = false
  }.let {
    KafkaConsumer(it, StringDeserializer(), StringDeserializer())
  }

}