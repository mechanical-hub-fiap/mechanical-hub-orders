package com.fiap.mechanicalhub.orders.infrastructure.messaging.commands

/** Mensagem fora do contrato: erro permanente, vai direto para a DLQ. */
class InvalidMessageException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
