package no.nav.dagpenger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class Rapid(
    rapidsConnection: RapidsConnection,
    private val aktørId: String
) : River.PacketListener {
    var vedtakErEndret = false

    init {
        River(rapidsConnection).apply {
            validate { it.demandAll("@event_name", listOf("vedtak_endret")) }
            validate { it.interestedIn("aktørId") }
            validate { it.requireValue("aktørId", aktørId) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        vedtakErEndret = true
    }
}
