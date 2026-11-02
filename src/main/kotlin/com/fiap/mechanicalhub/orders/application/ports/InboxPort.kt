package com.fiap.mechanicalhub.orders.application.ports

import com.fiap.mechanicalhub.orders.application.events.OutgoingEvent

/** `inbox_events`: deduplicacao dos comandos por `idempotencyKey`. */
interface InboxPort {
    fun find(messageKey: String): ProcessedCommand?

    fun register(
        messageKey: String,
        response: OutgoingEvent?,
    )
}
