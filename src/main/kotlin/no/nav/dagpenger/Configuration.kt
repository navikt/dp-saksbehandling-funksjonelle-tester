package no.nav.dagpenger

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.ConfigurationProperties.Companion.fromFile
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import java.io.File
import java.io.FileNotFoundException

private const val TOPIC = "privat-dagpenger-behov-v2"

private val localProperties = ConfigurationMap(
    mapOf(
        "database.host" to "localhost",
        "database.name" to "saksbehandling",
        "database.port" to "5432",
        "database.vault" to "postgresql/local/",
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
        "database.host" to "b27dbvl013.preprod.local",
        "database.name" to "saksbehandling",
        "database.port" to "5432",
        "database.vault" to "postgresql/preprod-fss/",
        "profile" to Profile.DEV.toString(),
        "kafka.bootstrap.servers" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
        "kafka.topic" to TOPIC,
        "kafka.reset.policy" to "latest"
    )
)

private fun config() = when (System.getenv("CUCUMBER_ENV") ?: System.getProperty("CUCUMBER_ENV")) {
    "dev" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding fromFile(File("/var/run/secrets/nais.io/test_data/brukere")) overriding devProperties
    else -> {
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
    }
}

object Configuration {
    val bootstrapServers = config()[Key("kafka.bootstrap.servers", stringType)]
    val database = Database()
    val topic = config()[Key("kafka.topic", stringType)]
    val resetPolicy = config()[Key("kafka.reset.policy", stringType)]

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
            "flere.arbeidsforhold.aktoerid" to config()[Key("flere.arbeidsforhold.aktoerid", stringType)],
            "happy.path.aktoerid" to config()[Key("happy.path.aktoerid", stringType)],
            "happy.path.fnr" to config()[Key("happy.path.fnr", stringType)]
        )

    data class Database(
        val host: String = config()[Key("database.host", stringType)],
        val port: String = config()[Key("database.port", stringType)],
        val name: String = config()[Key("database.name", stringType)],
        val vault: String = config()[Key("database.vault", stringType)]
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
