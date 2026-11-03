package com.fiap.mechanicalhub.orders.infrastructure.notification.channels

import com.fiap.mechanicalhub.orders.logging.FlowLog
import com.fiap.mechanicalhub.orders.logging.LogLevel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * WhatsApp, reaproveitado do `WhatsAppMessenger` do monolito: registra o envio
 * em log, sem o telefone do destinatario (logging-standard §6). A integracao
 * com a API real do WhatsApp fica pendente.
 */
@Component
class WhatsAppMessenger : NotificationChannel {
    override fun send(
        recipient: Recipient,
        message: NotificationMessage,
    ) {
        if (recipient.telephone == null) return
        FlowLog.note(log, LogLevel.DEBUG, "WhatsApp de notificacao enviado (envio simulado)", "channel" to "WHATSAPP")
    }

    private companion object {
        val log = LoggerFactory.getLogger(WhatsAppMessenger::class.java)
    }
}
