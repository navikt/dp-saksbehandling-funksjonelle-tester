package no.nav.dagpenger

import com.natpryce.konfig.* // ktlint-disable no-wildcard-imports
import com.natpryce.konfig.ConfigurationProperties.Companion.fromFile
import java.io.File
import java.io.FileNotFoundException

private const val TOPIC = "privat-dagpenger-behov-v2"

private val localProperties = ConfigurationMap(
        mapOf(
                "profile" to Profile.LOCAL.toString(),
                "kafka.topic" to TOPIC,
                "kafka.reset.policy" to "earliest",
                "username" to "localuser",
                "password" to "localuser",
                "kafka.bootstrap.servers" to "localhost:002",
                "nav.truststore.path" to "dummy",
                "nav.truststore.password" to "changeme"
        )
)
private val devProperties = ConfigurationMap(
        mapOf(
                "profile" to Profile.DEV.toString(),
                "kafka.bootstrap.servers" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
                "kafka.topic" to TOPIC,
                "kafka.reset.policy" to "latest",
                "username" to "/var/run/secrets/nais.io/service_user/username".readFile()!!,
                "password" to "/var/run/secrets/nais.io/service_user/password".readFile()!!
        )
)

private fun config() = when (System.getenv("CUCUMBER_ENV") ?: System.getProperty("CUCUMBER_ENV")) {
    "dev" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding fromFile(File("/var/run/secrets/nais.io/test_data/brukere")) overriding devProperties
    else -> {
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
    }
}

object Configuration {
    // val profile: Profile = config()[Key("profile", stringType)].let { Profile.valueOf(it) }
    val bootstrapServers = config()[Key("kafka.bootstrap.servers", stringType)]
    val topic = config()[Key("kafka.topic", stringType)]
    val resetPolicy = config()[Key("kafka.reset.policy", stringType)]

    val username = config()[Key("username", stringType)]
    val password = config()[Key("password", stringType)]

    val rapidApplication: Map<String, String> = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to bootstrapServers,
            "KAFKA_CONSUMER_GROUP_ID" to "dp-saksbehandling-funksjonelle-tester-tjafs2",
            "KAFKA_RAPID_TOPIC" to topic,
            "KAFKA_RESET_POLICY" to resetPolicy,
            "NAV_TRUSTSTORE_PATH" to config()[Key("nav.truststore.path", stringType)],
            "NAV_TRUSTSTORE_PASSWORD" to config()[Key("nav.truststore.password", stringType)]
    )

    val testbrukere: Map<String, String> =
            mapOf(
                    "flere.arbeidsforhold.fnr" to config()[Key("flere.arbeidsforhold.fnr", stringType)],
                    "flere.arbeidsforhold.aktoerid" to config()[Key("flere.arbeidsforhold.aktoerid", stringType)]
            )
}

enum class Profile {
    LOCAL, DEV
}

private fun String.readFile() =
        try {
            File(this).readText(Charsets.UTF_8)
        } catch (err: FileNotFoundException) {
            null
        }
