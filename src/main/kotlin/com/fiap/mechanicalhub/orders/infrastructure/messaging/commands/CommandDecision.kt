package com.fiap.mechanicalhub.orders.infrastructure.messaging.commands

import com.fiap.mechanicalhub.orders.application.notifications.Notification

/** O que o listener faz com a mensagem depois do processamento. */
sealed interface CommandDecision {
    /** COMMIT feito (ou duplicata): apagar a mensagem e, se houver, notificar. */
    data class Acknowledge(
        val notification: Notification?,
    ) : CommandDecision

    /** Erro transitorio: nao apagar; nova tentativa apos `delaySeconds`. */
    data class Retry(
        val delaySeconds: Int,
        val error: Throwable,
    ) : CommandDecision

    /** Erro permanente: enviar a DLQ e apagar da fila de origem. */
    data class DeadLetter(
        val error: Throwable,
    ) : CommandDecision
}
