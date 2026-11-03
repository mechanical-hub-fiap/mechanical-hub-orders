package com.fiap.mechanicalhub.orders.infrastructure.messaging.outbox

import java.time.Clock

/** Acesso ao `outbox_events` usado pelo relay. */
interface OutboxStore {
    /** Lideranca do ciclo: so a instancia que obtem o lock publica. */
    fun tryAcquireLeadership(): Boolean

    fun findPending(limit: Int): List<PendingOutboxMessage>

    fun markSent(
        sequence: Long,
        clock: Clock,
    )

    fun registerFailure(
        sequence: Long,
        error: String,
    )
}
