package sample

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.internals.NoOpConsumerRebalanceListener
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Executors

/**
 * [consumer] must not be closed explicitly
 */
abstract class AtLeastOnceProcessor<ConsumerKey, ConsumerValue>(
  private val consumer: Consumer<ConsumerKey, ConsumerValue>,
  private val subscribeTopics: List<String>,
  private val pollTimeout: Duration,
  private val rebalanceListener: ConsumerRebalanceListener = NoOpConsumerRebalanceListener(),
  private val respawnTimeout: Duration = Duration.ofSeconds(3)
) {

  private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
  private val logger = LoggerFactory.getLogger(javaClass)

  private var parentJob: Job? = null

  fun start(): Boolean {
    parentJob = GlobalScope.launch(dispatcher) {
      consumer.use { consumer ->
        while (true) {
          try {
            consumer.subscribe(subscribeTopics, rebalanceListener)
            spawnWorker()
            yield()
          } catch (e: CancellationException) {
            logger.info("Shutting down processor for group [${consumer}]")
            return@launch
          } catch (e: Throwable) {
            consumer.unsubscribe()
            logger.error(
              "Handled error in consumer loop, job will be re-spawned in $respawnTimeout millis",
              e
            )
            delay(respawnTimeout.toMillis())
          }
        }
      }
    }
    return parentJob!!.start()
  }

  private suspend fun spawnWorker() = coroutineScope {
    launch {
      logger.debug("Spawning consumer-worker")
      while (true) {
        val consumerRecords = consumer.poll(pollTimeout)
        if (consumerRecords.isEmpty) {
          yield()
          continue
        }
        withContext(Dispatchers.Default) {
          processing(consumerRecords)
        }
        consumer.commitSync()
        yield()
      }
    }
  }

  protected abstract suspend fun processing(
    records: ConsumerRecords<ConsumerKey, ConsumerValue>
  )

  fun stop(): Boolean {
    runBlocking {
      parentJob?.cancelAndJoin()
    }
    dispatcher.close()
    return true
  }
}