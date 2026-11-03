package com.fiap.mechanicalhub.orders.infrastructure.messaging.outbox

/** Envio sincrono para a fila FIFO do orquestrador. */
fun interface OutboxMessagePublisher {
    fun publish(message: PendingOutboxMessage)
}
