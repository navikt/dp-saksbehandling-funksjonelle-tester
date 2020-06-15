#language: no
Egenskap: Saksbehandling

  Scenario: Flere arbeidsforhold gir manuell behandling
    Gitt en søker med aktørid "flere.arbeidsforhold.aktoerid" og fødselsnummer "flere.arbeidsforhold.fnr"
    Når vi skal vurdere søknaden
    Så må søknaden for aktørid "flere.arbeidsforhold.aktoerid" manuelt behandles

  Scenario: Søknad som kan gi automatisk innvilgelse
    Gitt en søker med aktørid "happy.path.aktoerid" og fødselsnummer "happy.path.fnr"
    Når vi skal vurdere søknaden
    Så kan søknaden for aktørid ""happy.path.aktoerid" automatisk innvilges