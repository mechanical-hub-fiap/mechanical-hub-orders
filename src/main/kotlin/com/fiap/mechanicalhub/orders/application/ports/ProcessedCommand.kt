package com.fiap.mechanicalhub.orders.application.ports

import com.fiap.mechanicalhub.orders.application.events.OutgoingEvent

/** Comando ja processado; `response` e o evento publicado na primeira vez (se houve). */
data class ProcessedCommand(
    val response: OutgoingEvent?,
)
