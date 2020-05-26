package no.nav.dagpenger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.huxhorn.sulky.ulid.ULID
import io.cucumber.java8.No
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private val log = KotlinLogging.logger {}

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
            log.info { "publiserte søknadsmessage" }
        }

        Så("må søknaden for aktørid {string} manuelt behandles") { aktørId: String ->

            val messages = mutableListOf<JsonMessage>()

            log.info { "setting up rapid rapid" }

            object : River.PacketListener {
                init {
                    River(rapidsConnection).apply {
                        validate { it.demandAll("@event_name", listOf("Søknad")) }
                        // @todo validér aktørId og riktig state
                    }.register(this)
                }

                override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
                    messages.add(packet)
                }
            }
            log.info { "starting rapid" }

            GlobalScope.launch { rapidsConnection.start() }

            log.info { "2s delay" }
            runBlocking { delay(2000) }
            log.info { "finished waiting" }

            rapidsConnection.stop()
            log.info { "messages size: ${messages.size}" }
            messages.size shouldNotBe 0
        }
    }
}
