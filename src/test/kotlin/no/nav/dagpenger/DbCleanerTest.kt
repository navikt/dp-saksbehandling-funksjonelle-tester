package no.nav.dagpenger.no.nav.dagpenger

import com.zaxxer.hikari.HikariDataSource
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.cleanMelding
import no.nav.dagpenger.cleanUser
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer

internal class DbCleanerTest {

    internal object PostgresContainer {
        val instance by lazy {
            PostgreSQLContainer<Nothing>("postgres:12.1").apply {
                this.withInitScript("init.sql")
                start()
            }
        }
    }

    internal object DataSource {
        val instance: HikariDataSource by lazy {
            HikariDataSource().apply {
                username = PostgresContainer.instance.username
                password = PostgresContainer.instance.password
                jdbcUrl = PostgresContainer.instance.jdbcUrl
                connectionTimeout = 1000L
            }
        }
    }

    private fun insertPerson(fnr: String) = using(sessionOf(DataSource.instance)) { session ->
        session.run(queryOf("INSERT INTO PERSON(fnr) VALUES (?)", fnr).asUpdate)
    }

    private fun insertMelding(fnr: String) = using(sessionOf(DataSource.instance)) { session ->
        session.run(queryOf("INSERT INTO MELDING(fnr) VALUES (?)", fnr).asUpdate)
    }

    @Test
    fun `Delete from person table`() {
        insertPerson("fnr1")
        insertPerson("fnr2")
        insertPerson("fnr2")

        cleanUser(DataSource.instance, "fnr1") shouldBe 1
        cleanUser(DataSource.instance, "fnr2") shouldBe 2
        cleanUser(DataSource.instance, "fnr1") shouldBe 0
    }

    @Test
    fun `Delete from melding table`() {
        insertMelding("fnr1")
        insertMelding("fnr2")
        insertMelding("fnr2")

        cleanMelding(DataSource.instance, "fnr1") shouldBe 1
        cleanMelding(DataSource.instance, "fnr2") shouldBe 2
        cleanMelding(DataSource.instance, "fnr1") shouldBe 0
    }
}
