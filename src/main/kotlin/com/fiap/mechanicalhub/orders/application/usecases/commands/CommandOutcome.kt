package com.fiap.mechanicalhub.orders.application.usecases.commands

import com.fiap.mechanicalhub.orders.application.notifications.Notification

sealed interface CommandOutcome {
    data class Applied(
        val notification: Notification?,
    ) : CommandOutcome

    /** Ja processado: nada reaplicado (a resposta original, se houve, e regravada no outbox). */
    data object Duplicate : CommandOutcome
}
