package no.nav.dagpenger

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

internal fun dataSourceFrom(config: Configuration): HikariDataSource = HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
    HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://${config.database.host}:${config.database.port}/${config.database.name}"
        maximumPoolSize = 2
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
    },
    config.database.vault,
    "${config.database.name}-user"
)

internal fun cleanDb() {
    val ds = dataSourceFrom(Configuration)

    Configuration.testbrukere.filter { it.key.contains("fnr") }.forEach { entry ->
        cleanUser(ds, entry.value).also { log.info { "Personer slettet(${entry.key}): $it" } }
        cleanMelding(ds, entry.value).also { log.info { "Meldinger slettet(${entry.key}): $it" } }
    }
}

internal fun cleanUser(ds: DataSource, fnr: String): Int {
    val statement =
        """DELETE FROM person WHERE fnr = ?"""
    return using(sessionOf(ds)) {
        it.run(queryOf(statement, fnr).asUpdate)
    }
}

internal fun cleanMelding(ds: DataSource, fnr: String): Int {
    val statement =
        """DELETE FROM melding WHERE fnr = ?"""
    return using(sessionOf(ds)) {
        it.run(queryOf(statement, fnr).asUpdate)
    }
}
