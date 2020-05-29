package no.nav.dagpenger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.huxhorn.sulky.ulid.ULID
import io.cucumber.java8.No
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.*
import org.awaitility.kotlin.await
import java.time.Duration
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

class SaksbehandlingSteps() : No {
    private lateinit var søknad: Map<String, String>

    companion object {

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
                            log.info { "packet found" }
                            messages.add(packet)
                        }


                        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
                            log.error { "Problems!! ->  ${problems}" }

                        }

                        override fun onSevere(error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
                            log.error(error) { "Something bad happen" }
                        }
                    }
                }.also {
                    GlobalScope.launch { it.start() }
                }
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

            log.info { "venter på pakker" }

            await.atMost(Duration.ofMinutes(5L)).untilAsserted {
                messages
                        .filter { it["aktørId"].asText() == aktørId }
                        .filter { it["@event_name"].asText() == "vedtak_endret" }
                        .size shouldBeGreaterThan 0
            }
            log.info { "finished" }
            log.info { "messages size: ${messages.size}" }

        }
    }
}
