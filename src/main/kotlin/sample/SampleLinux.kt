package sample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@SpringBootApplication
@ConfigurationPropertiesScan
class SampleLinux(
    private val documentReceiver: DocumentReceiver
) {

    @PostConstruct
    fun start() {
        documentReceiver.start()
    }

    @PreDestroy
    fun stop() {
        documentReceiver.stop()
    }

}

fun main(args: Array<String>) {
    runApplication<SampleLinux>(*args)
}