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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.awaitility.kotlin.await

private val log = KotlinLogging.logger {}

class SaksbehandlingSteps() : No {
    private lateinit var søknad: Map<String, String>

    private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    private val messages = mutableListOf<JsonMessage>()
    private val rapidsConnection = RapidApplication.Builder(
            RapidApplication.RapidApplicationConfig.fromEnv(Configuration.rapidApplication)
    ).build()
            .also {
                object : River.PacketListener {
                    init {
                        River(it).register(this)
                    }

                    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
                        packet.interestedIn("aktørId", "gjeldendeTilstand")
                        messages.add(packet)
                    }
                }
            }.also {
                GlobalScope.launch { it.start() }
            }

    fun sendToRapid(behov: Map<*, *>) {
        rapidsConnection.publish(objectMapper.writeValueAsString(behov))
    }

    init {
        Gitt("en søker med aktørid {string}") { aktørId: String ->
            søknad = mapOf(
                    "@id" to ULID().nextULID(),
                    "@event_name" to "Søknad",
                    "@opprettet" to LocalDateTime.now().toString(),
                    "fødselsnummer" to "***REMOVED***",
                    "aktørId" to aktørId,
                    "behandlingId" to "GYLDIG_SOKNAD"
            )
        }

        Når("vi skal vurdere søknaden") {
            sendToRapid(søknad)
            log.info { "publiserte søknadsmessage" }
        }

        Så("må søknaden for aktørid {string} manuelt behandles") { aktørId: String ->

            log.info { "venter på pakker" }

            await.atMost(Duration.ofMinutes(5L)).untilAsserted {
                messages.toList()
                        .filter { it["aktørId"].asText() == aktørId }
                        .any { it["gjeldendeTilstand"].asText() == "TilArena" } shouldBe true
            }
            log.info { "finished" }
            log.info { "messages size: ${messages.size}" }
        }
    }
}
