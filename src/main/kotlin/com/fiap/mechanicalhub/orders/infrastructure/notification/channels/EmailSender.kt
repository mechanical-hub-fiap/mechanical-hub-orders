package com.fiap.mechanicalhub.orders.infrastructure.notification.channels

import com.fiap.mechanicalhub.orders.logging.FlowLog
import com.fiap.mechanicalhub.orders.logging.LogLevel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * E-mail, reaproveitado do `EmailSender` do monolito: registra o envio em log.
 * A integracao com um provedor real de e-mail fica pendente (acesso externo).
 * O log nao leva destinatario nem corpo (logging-standard §6): o corpo tem o
 * link com o token de aprovacao.
 */
@Component
class EmailSender : NotificationChannel {
    override fun send(
        recipient: Recipient,
        message: NotificationMessage,
    ) {
        if (recipient.email == null) return
        FlowLog.note(log, LogLevel.DEBUG, "E-mail de notificacao enviado (envio simulado)", "channel" to "EMAIL")
    }

    private companion object {
        val log = LoggerFactory.getLogger(EmailSender::class.java)
    }
}
