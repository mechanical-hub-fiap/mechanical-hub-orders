package com.fiap.mechanicalhub.orders.application.usecases.commands

import com.fiap.mechanicalhub.orders.application.events.OutgoingEvent
import com.fiap.mechanicalhub.orders.application.notifications.Notification

/** Efeito de um comando: resposta para o outbox e notificacao para depois do COMMIT. */
data class CommandEffect(
    val response: OutgoingEvent? = null,
    val notification: Notification? = null,
)
