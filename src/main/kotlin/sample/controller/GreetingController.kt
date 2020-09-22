package sample.controller

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController;
import sample.KafkaProperties
import sample.configuration.SendBatch
import sample.configuration.TransactionalSender
import java.util.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@RestController
class GreetingController {

  @Autowired
  private lateinit var logger: Logger;

  @Autowired
  private lateinit var transactionalSender: TransactionalSender<String, String>

  @Autowired
  private lateinit var kafkaProperties: KafkaProperties

  @PostConstruct
  fun schedule() {
    transactionalSender.start()
  }

  @PreDestroy
  fun preDestroy() {
    transactionalSender.close()
  }

  @GetMapping("/greeting")
  fun greeting(@RequestParam(value = "ids") ids: List<String> ) {
//    listOf("first", "second")
    val send = send(ids)
  }

  private fun send(ids: List<String>) = runBlocking {
    logger.debug("Send ids $ids to topic ${kafkaProperties.chunks.topic.consumer}")

    val input = Channel<ProducerRecord<String, String>>(Channel.UNLIMITED)
    ids.forEach {
      val record = ProducerRecord(
        kafkaProperties.chunks.topic.consumer,
        getUUID(it),
        "$it+000"
      )
      input.send(record)
    }

    input.close()

    val output = Channel<RecordMetadata>(Channel.UNLIMITED)

    val sendResult = transactionalSender.send(SendBatch(input, output))
    if (!sendResult) {
      logger.error("The request was backpressured due to kafka transaction sender")
      throw RuntimeException("The request was backpressured")
    }

    output.toList()
  }

  fun getUUID(internalId: String) = internalId.take(Companion.UUIDLength)

  companion object {
    private const val UUIDLength = 36
  }
}