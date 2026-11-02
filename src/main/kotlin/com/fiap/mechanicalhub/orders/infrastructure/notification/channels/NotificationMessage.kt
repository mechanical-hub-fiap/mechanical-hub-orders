package com.fiap.mechanicalhub.orders.infrastructure.notification.channels

/** Mensagem pronta para envio (mesmo formato do monolito: assunto + corpo). */
data class NotificationMessage(
    val subject: String,
    val body: String,
)
