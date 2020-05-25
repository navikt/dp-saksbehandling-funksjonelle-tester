package no.nav.dagpenger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.huxhorn.sulky.ulid.ULID
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import no.nav.helse.rapids_rivers.RapidApplication

class SaksbehandlingSteps : No {
    private lateinit var rapid: Rapid
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
        Gitt("en søker med aktør id {string}") { aktørId: String ->
            søknad = mapOf(
                    "@id" to ULID().nextULID(),
                    "@event_name" to "Søknad",
                    "@opprettet" to LocalDateTime.now().toString(),
                    "fødselsnummer" to "12345678910",
                    "aktørId" to aktørId,
                    "behandlingId" to ULID().nextULID()
            )

            rapid = Rapid(rapidsConnection, aktørId)
        }

        Når("vi skal vurdere søknaden") {
            sendToRapid(søknad)
            Thread.sleep(10000)
        }

        Så("må søknaden manuelt behandles") {
            rapid.vedtakErEndret shouldBe true
        }
    }
}
