package com.fiap.mechanicalhub.orders.infrastructure.notification

import com.fiap.mechanicalhub.orders.application.notifications.Notification
import com.fiap.mechanicalhub.orders.application.ports.CustomerRepository
import com.fiap.mechanicalhub.orders.application.ports.NotificationPort
import com.fiap.mechanicalhub.orders.infrastructure.config.OrdersProperties
import com.fiap.mechanicalhub.orders.infrastructure.notification.channels.NotificationChannel
import com.fiap.mechanicalhub.orders.infrastructure.notification.channels.NotificationMessage
import com.fiap.mechanicalhub.orders.infrastructure.notification.channels.Recipient
import com.fiap.mechanicalhub.orders.infrastructure.persistence.repositories.EmployeeJpaRepository
import com.fiap.mechanicalhub.orders.logging.FlowFields
import com.fiap.mechanicalhub.orders.logging.FlowLog
import com.fiap.mechanicalhub.orders.logging.LogLevel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Envia a notificacao por todos os canais, depois do COMMIT do comando.
 * Falha em qualquer etapa (destinatario, canal) so gera log: o comando ja foi
 * confirmado e nao e desfeito nem retentado.
 */
@Component
class NotificationAdapter(
    private val customerRepository: CustomerRepository,
    private val employeeRepository: EmployeeJpaRepository,
    private val channels: List<NotificationChannel>,
    properties: OrdersProperties,
) : NotificationPort {
    private val messages = NotificationMessages(properties.frontendBaseUrl)

    override fun send(notification: Notification) {
        runCatching { compose(notification) }
            .onFailure { write(notification, LogLevel.ERROR, "Falha ao montar a notificacao", it) }
            .getOrNull()
            ?.let { (recipient, message) -> deliver(notification, recipient, message) }
    }

    private fun compose(notification: Notification): Pair<Recipient, NotificationMessage>? =
        when (notification) {
            is Notification.BudgetReady -> {
                customer(notification.customerId)?.let { it to messages.budgetReady(notification, it.name) }
            }

            is Notification.PaymentLinkReady -> {
                customer(notification.customerId)?.let { it to messages.paymentLinkReady(notification, it.name) }
            }

            is Notification.OrderReleased -> {
                notification.responsibleUserId?.let(::employee)?.let { it to messages.orderReleased(notification, it.name) }
            }
        }

    private fun deliver(
        notification: Notification,
        recipient: Recipient,
        message: NotificationMessage,
    ) {
        channels.forEach { channel ->
            val channelName = channel.javaClass.simpleName
            runCatching { channel.send(recipient, message) }
                .onSuccess { write(notification, LogLevel.INFO, "Notificacao enviada", null, "channel" to channelName) }
                .onFailure { write(notification, LogLevel.ERROR, "Falha no envio da notificacao", it, "channel" to channelName) }
        }
    }

    private fun customer(customerId: UUID): Recipient? {
        val customer = customerRepository.findById(customerId)
        if (customer == null) {
            FlowLog.note(log, LogLevel.WARN, "Cliente nao encontrado para a notificacao", "customerId" to customerId)
        }
        return customer?.let { Recipient(it.name, it.email, it.telephone) }
    }

    private fun employee(userId: UUID): Recipient? {
        val employee = employeeRepository.findByIdAndDeletedAtIsNull(userId)
        if (employee == null) {
            FlowLog.note(log, LogLevel.WARN, "Funcionario nao encontrado para a notificacao", "userId" to userId)
        }
        return employee?.let { Recipient(it.name, it.email, telephone = null) }
    }

    /**
     * A notificacao sai depois do COMMIT do comando que a gerou; a linha leva o `status` desse comando para aparecer
     * no filtro da etapa. Sem destinatario, contato nem corpo (logging-standard §6).
     */
    private fun write(
        notification: Notification,
        level: LogLevel,
        msg: String,
        error: Throwable?,
        vararg context: Pair<String, Any?>,
    ) {
        val fields =
            FlowFields(
                orderId = notification.orderId,
                status = STATUS_BY_NOTIFICATION[notification.javaClass],
                msg = msg,
                context = listOf<Pair<String, Any?>>("notification" to notification.javaClass.simpleName) + context,
                error = error,
            )
        FlowLog.write(log, level, fields)
    }

    private companion object {
        val log = LoggerFactory.getLogger(NotificationAdapter::class.java)
        val STATUS_BY_NOTIFICATION =
            mapOf(
                Notification.BudgetReady::class.java to "SEND_APPROVAL",
                Notification.PaymentLinkReady::class.java to "SEND_PAYMENT_LINK",
                Notification.OrderReleased::class.java to "RELEASE_ORDER",
            )
    }
}
