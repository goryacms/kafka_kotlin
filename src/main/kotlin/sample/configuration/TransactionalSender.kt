package sample.configuration

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import java.util.concurrent.Executors

/**
 * Creates transactional wrapper around given [Producer]
 * Note, producer instance MUST NOT be shared within other senders and threads,
 * when this sender could be used from different threads.
 * All requests up to [inputChannel] capacity will be placed onto outgoing queue
 * in non-blocking fashion, however all calls to [send] must be checked for returned value
 * and produce corresponding error, retry or backpressure signal to upstream.
 * Records are sent to topic in random order.
 */
class TransactionalSender<K, V>(
  private val kafkaProducer: Producer<K, V>,
  private val batchOfBatchSize: Int = 1
) {

  private val inputChannel: Channel<SendBatch<K, V>> = Channel(2048)
  private val txOperationsDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
  private lateinit var sender: Job
  private var channelOpen: Boolean = false

  /**
   * Requests to send given records batch to kafka.
   * When result is ready - all records metadata will be published to [SendBatch]'s `outputChannel`
   * If transactional producer will fail to commit running transaction it'll close `outputChannel`
   * with corresponding error.
   *
   * @return `true` if request was successfully placed or `false`, when request was backpressured
   */
  fun send(batch: SendBatch<K, V>): Boolean {
    return inputChannel.offer(batch)
  }

  fun start() {
    sender = GlobalScope.launch(txOperationsDispatcher) {
      kafkaProducer.initTransactions()
      channelOpen = true

      while (channelOpen) {
        val buffer = mutableListOf<SendBatch<K, V>>()
        for (batchIdx in 0 until batchOfBatchSize) {
          buffer += inputChannel.poll() ?: break
        }
        if (buffer.isEmpty()) {
          delay(10)
          continue
        }

        try {
          kafkaProducer.beginTransaction()

          withContext(Dispatchers.IO) {
            buffer.forEach { batch ->
              launch {
                val results = kafkaProducer.sendChannel(batch.inputChannel)
                results.forEach { batch.outputChannel.offer(it) }
              }
            }
          }

          kafkaProducer.commitTransaction()
          buffer.forEach { it.outputChannel.close() }
        } catch (e: Exception) {
          try {
            kafkaProducer.abortTransaction()
          } finally {
            buffer.forEach { it.outputChannel.close(e) }
          }
        }
      }
    }
  }

  private suspend fun sendRecord(
    record: ProducerRecord<K, V>,
    batch: SendBatch<K, V>
  ) {
    val metadata = kafkaProducer.sendRecord(record)
    batch.outputChannel.offer(metadata)
  }

  fun close() = runBlocking {
    inputChannel.close()
    channelOpen = false
    sender.cancelAndJoin()
    txOperationsDispatcher.close()
  }
}

/**
 * `inputChannel` and `outputChannel` always MUST BE of type [Channel.Factory.UNLIMITED]
 * in order to not block producer sending coroutines.
 * Otherwise, producer will skip backpressured invocations of send()
 */
data class SendBatch<K, V>(
  val inputChannel: Channel<ProducerRecord<K, V>>,
  val outputChannel: Channel<RecordMetadata>
)