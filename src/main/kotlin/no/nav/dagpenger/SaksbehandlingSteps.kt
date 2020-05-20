package no.nav.dagpenger

import io.cucumber.java8.No

class SaksbehandlingSteps : No {

    init {
        Gitt("en søker med aktør id {string}") {
            aktørId: String -> "abc"
        }

        Når("vi skal vurdere søknaden") { }

        Så("må søknaden manuelt behandles") {
           assert(true)
        }
    }
}