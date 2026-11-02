package com.fiap.mechanicalhub.orders.infrastructure.notification.channels

import com.fiap.mechanicalhub.orders.logging.FlowLog
import com.fiap.mechanicalhub.orders.logging.LogLevel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** Mensagem pronta para envio (mesmo formato do monolito: assunto + corpo). */
data class NotificationMessage(
    val subject: String,
    val body: String,
)

/** Destinatario com os contatos disponiveis (cliente ou funcionario). */
data class Recipient(
    val name: String,
    val email: String?,
    val telephone: String?,
)

/** Canal de envio. Pode lancar: quem chama trata a falha (so log). */
interface NotificationChannel {
    fun send(
        recipient: Recipient,
        message: NotificationMessage,
    )
}

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
