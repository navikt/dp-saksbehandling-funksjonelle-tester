package no.nav.dagpenger

import io.cucumber.core.cli.Main
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

val args = arrayOf(
    "--threads", "2",
    "-g", "no.nav.dagpenger",
    "-p", "pretty",
    "classpath:features",
    "--tags", "not @ignored "
)

fun main() {
    cleanDb()
    Main.main(*args)
}
