package no.nav.dagpenger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.huxhorn.sulky.ulid.ULID
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.awaitility.kotlin.await
import java.time.Duration
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

class SaksbehandlingSteps() : No {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        private val messages = mutableListOf<JsonMessage>()
        private val rapidsConnection = RapidApplication.Builder(
            RapidApplication.RapidApplicationConfig.fromEnv(Configuration.rapidApplication)
        ).build()
            .also { rapidsConnection ->
                object : River.PacketListener {
                    init {
                        River(rapidsConnection).apply {
                            validate { it.requireAny("aktørId", Configuration.testbrukere.values.toList()) }
                            validate { it.requireAny("gjeldendeTilstand", listOf("TilArena", "VedtakFattet")) }
                        }.register(this)
                    }

                    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
                        messages.add(packet)
                    }
                }
            }.also {
                GlobalScope.launch { it.start() }
            }

        fun sendToRapid(behov: Map<*, *>) {
            rapidsConnection.publish(objectMapper.writeValueAsString(behov))
        }
    }

    private lateinit var søknad: Map<String, Any>

    init {
        Gitt("en søker med aktørid {string} og fødselsnummer {string}") { aktørIdKey: String, fødselsnummerKey: String ->
            val id = ULID().nextULID()
            val søknadsId = "GYLDIG_SOKNAD"
            søknad = mapOf (
                "@id" to id,
                "@event_name" to "Søknad",
                "@opprettet" to LocalDateTime.now().toString(),
                "tvingNyBehandling" to true,
                "fødselsnummer" to Configuration.testbrukere[fødselsnummerKey]!!,
                "aktørId" to Configuration.testbrukere[aktørIdKey]!!,
                "søknadsId" to søknadsId
            )
            log.info { "lager søknad for $aktørIdKey med id $id og søknadsid $søknadsId" }
        }

        Når("vi skal vurdere søknaden") {
            sendToRapid(søknad)
        }

        Så("må søknaden for aktørid {string} manuelt behandles") { aktørIdKey: String ->
            await.atMost(Duration.ofSeconds(30L)).untilAsserted {
                messages.toList()
                    .filter { it["aktørId"].asText() == Configuration.testbrukere[aktørIdKey] }
                    .any { it["gjeldendeTilstand"].asText() == "TilArena" } shouldBe true
            }
        }

        Så("kan søknaden for aktørid {string} automatisk innvilges") { aktørIdKey: String ->
            await.atMost(Duration.ofSeconds(30L)).untilAsserted {
                messages.toList()
                    .filter { it["aktørId"].asText() == Configuration.testbrukere[aktørIdKey] }
                    .any { it["gjeldendeTilstand"].asText() == "VedtakFattet" } shouldBe true
            }
        }
    }
}
