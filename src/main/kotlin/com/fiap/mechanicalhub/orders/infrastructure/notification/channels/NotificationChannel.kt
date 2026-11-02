package com.fiap.mechanicalhub.orders.infrastructure.notification.channels

/** Canal de envio. Pode lancar: quem chama trata a falha (so log). */
interface NotificationChannel {
    fun send(
        recipient: Recipient,
        message: NotificationMessage,
    )
}
