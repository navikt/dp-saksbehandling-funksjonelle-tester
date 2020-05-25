package no.nav.dagpenger

import com.natpryce.konfig.* // ktlint-disable no-wildcard-imports

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
                "kafka.reset.policy" to "latest"
        )
)

private fun config() = when (System.getenv("CUCUMBER_ENV") ?: System.getProperty("CUCUMBER_ENV")) {
    "dev" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties
    else -> {
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
    }
}

object Configuration {
    val profile: Profile = config()[Key("profile", stringType)].let { Profile.valueOf(it) }
    val bootstrapServers = config()[Key("kafka.bootstrap.servers", stringType)]
    val topic = config()[Key("kafka.topic", stringType)]

    val rapidApplication: Map<String, String> = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to bootstrapServers,
            "KAFKA_CONSUMER_GROUP_ID" to "dp-saksbehandling-funksjonelle-tester",
            "KAFKA_RAPID_TOPIC" to topic,
            "KAFKA_RESET_POLICY" to config()[Key("kafka.reset.policy", stringType)],
            "NAV_TRUSTSTORE_PATH" to config()[Key("nav.truststore.path", stringType)],
            "NAV_TRUSTSTORE_PASSWORD" to config()[Key("nav.truststore.password", stringType)]
    )
}

enum class Profile {
    LOCAL, DEV
}
