package sample.configuration

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.CLIENT_ID_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.COMPRESSION_TYPE_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.MAX_REQUEST_SIZE_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.TRANSACTIONAL_ID_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.TRANSACTION_TIMEOUT_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import sample.KafkaProperties
import java.net.InetAddress
import java.time.Duration
import java.util.Properties

@Configuration
class KafkaConfig(
  val kafkaProps: KafkaProperties
) {

  @Bean
  fun transactionalSender(): TransactionalSender<String, String> {
    val hostName = InetAddress.getLocalHost().hostName
    val properties = Properties()
    properties[BOOTSTRAP_SERVERS_CONFIG] = kafkaProps.chunks.bootstrapServers
    properties[MAX_REQUEST_SIZE_CONFIG] = 10485760
    properties[CLIENT_ID_CONFIG] = "document-balancer.chunks-producer.$hostName"
    properties[ENABLE_IDEMPOTENCE_CONFIG] = true
    properties[TRANSACTIONAL_ID_CONFIG] = "document-balancer.chunks-producer.$hostName"
    properties[TRANSACTION_TIMEOUT_CONFIG] = Duration.ofSeconds(30).toMillis().toInt()
    properties[KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
    properties[VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
    properties[COMPRESSION_TYPE_CONFIG] = "lz4"

    return TransactionalSender(KafkaProducer(properties))
  }
}