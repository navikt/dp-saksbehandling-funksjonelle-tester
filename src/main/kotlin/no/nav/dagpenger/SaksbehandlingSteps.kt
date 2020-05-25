package no.nav.dagpenger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.huxhorn.sulky.ulid.ULID
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.LocalDateTime
import java.util.Properties
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer

class SaksbehandlingSteps() : No {
    private lateinit var søknad: Map<String, String>

    private val rapidsConnection = RapidApplication.Builder(
            RapidApplication.RapidApplicationConfig.fromEnv(Configuration.rapidApplication)
    ).build()

    private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    fun sendToRapid(behov: Map<*, *>) {
        rapidsConnection.publish(objectMapper.writeValueAsString(behov))
    }

    init {
        Gitt("en søker med aktørid {string}") { aktørId: String ->
            søknad = mapOf(
                    "@id" to ULID().nextULID(),
                    "@event_name" to "Søknad",
                    "@opprettet" to LocalDateTime.now().toString(),
                    "fødselsnummer" to "12345678910",
                    "aktørId" to aktørId,
                    "behandlingId" to ULID().nextULID()
            )
        }

        Når("vi skal vurdere søknaden") {
            sendToRapid(søknad)
            Thread.sleep(10000)
        }

        Så("må søknaden for aktørid {string} manuelt behandles") { aktørId: String ->
            val consumer = createConsumer(Configuration.bootstrapServers)
            consumer.subscribe(listOf(Configuration.topic))

            val records = consumer.poll(Duration.ofSeconds(3L))

            records.map { objectMapper.readTree(it.value()) }
                    .filter { it["@event_name"].asText() == "vedtak_endret" }
                    .any { it["aktørId"].asText() == aktørId } shouldBe true
        }
    }

    private fun createConsumer(brokers: String): Consumer<String, String> {
        val props = Properties()
        props["bootstrap.servers"] = brokers
        props["group.id"] = "dp-saksbehandling-funksjonelle-tester-kodd"
        props["key.deserializer"] = StringDeserializer::class.java
        props["value.deserializer"] = StringDeserializer::class.java
        return KafkaConsumer<String, String>(props)
    }
}
