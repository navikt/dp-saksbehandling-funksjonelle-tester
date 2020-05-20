package no.nav.dagpenger

import io.cucumber.core.cli.Main

val args = arrayOf(
    "-g", "no.nav.dagpenger",
    "-p", "pretty",
    "--strict",
    "classpath:features",
    "--tags", "not @ignored ")

fun main() {
    Main.main(args)
}
