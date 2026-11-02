package com.fiap.mechanicalhub.orders.infrastructure.notification.channels

/** Destinatario com os contatos disponiveis (cliente ou funcionario). */
data class Recipient(
    val name: String,
    val email: String?,
    val telephone: String?,
)
