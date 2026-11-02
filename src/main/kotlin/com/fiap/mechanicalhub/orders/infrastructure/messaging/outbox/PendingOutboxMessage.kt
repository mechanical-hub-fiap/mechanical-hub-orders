package com.fiap.mechanicalhub.orders.infrastructure.messaging.outbox

import java.util.UUID

/** Linha `PENDING` do outbox pronta para envio. */
data class PendingOutboxMessage(
    val sequence: Long,
    val eventId: UUID,
    val aggregateId: UUID,
    val eventType: String,
    val payload: String,
)
