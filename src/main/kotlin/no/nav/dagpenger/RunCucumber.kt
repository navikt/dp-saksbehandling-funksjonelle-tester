package no.nav.dagpenger

import io.cucumber.core.cli.Main

val args = arrayOf(
    "--threads", "2",
    "-g", "no.nav.dagpenger",
    "-p", "pretty",
    "classpath:features",
    "--tags", "not @ignored ")

fun main() {
    Main.main(*args)
}
