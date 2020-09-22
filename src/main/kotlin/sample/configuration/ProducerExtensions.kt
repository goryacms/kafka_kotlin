package sample.configuration

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.joinAll
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Sends given record to classic producer in non-blocking way with suspension mechanism
 */
suspend fun <K, V> Producer<K, V>.sendRecord(record: ProducerRecord<K, V>): RecordMetadata =
  suspendCoroutine { continuation ->
    this.send(record) { metadata, exception ->
      if (exception != null) {
        continuation.resumeWithException(exception)
      } else {
        continuation.resume(metadata)
      }
    }
  }

/**
 * sends entries from the channel to Kafka, and waits for responses from all FutureRecordMetadata
 * avoid endless channels, we may get a OutOfMemoryError
 */
@Deprecated(
  message = "In favour of sendChannel",
  replaceWith = ReplaceWith("this.sendChannel(channel)")
)
suspend fun <K, V> Producer<K, V>.sendBatch(
  channel: ReceiveChannel<ProducerRecord<K, V>>
) {
  val list = mutableListOf<CompletableDeferred<RecordMetadata>>()
  for (record in channel) {
    val deferred = CompletableDeferred<RecordMetadata>()
    send(record) { result, exception ->
      if (exception != null) {
        deferred.completeExceptionally(exception)
      } else {
        deferred.complete(result)
      }
    }
    list.add(deferred)
  }
  list.joinAll()
}

/**
 * sends entries from the channel to Kafka, and waits for responses from all FutureRecordMetadata
 * avoid endless channels, we may get a OutOfMemoryError
 */
suspend fun <K, V> Producer<K, V>.sendChannel(
  channel: ReceiveChannel<ProducerRecord<K, V>>
): List<RecordMetadata> {
  val list = mutableListOf<CompletableDeferred<RecordMetadata>>()
  for (record in channel) {
    val deferred = CompletableDeferred<RecordMetadata>()
    send(record) { result, exception ->
      if (exception != null) {
        deferred.completeExceptionally(exception)
      } else {
        deferred.complete(result)
      }
    }
    list.add(deferred)
  }
  return list.awaitAll()
}