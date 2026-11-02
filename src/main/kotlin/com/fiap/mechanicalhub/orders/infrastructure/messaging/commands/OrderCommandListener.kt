package com.fiap.mechanicalhub.orders.infrastructure.messaging.commands

import com.fiap.mechanicalhub.orders.application.ports.NotificationPort
import com.fiap.mechanicalhub.orders.infrastructure.observability.LogContext
import com.fiap.mechanicalhub.orders.logging.FlowFields
import com.fiap.mechanicalhub.orders.logging.FlowLog
import com.fiap.mechanicalhub.orders.logging.LogLevel
import com.fiap.mechanicalhub.orders.logging.Stage
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.SqsHeaders
import io.awspring.cloud.sqs.listener.Visibility
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

/**
 * Consumidor de `mechanical-hub-orders-commands.fifo` (confirmacao manual).
 * A mensagem so e apagada apos o COMMIT do caso de uso; em erro transitorio
 * ela fica na fila, bloqueando o grupo (`orderId`) ate a proxima tentativa.
 */
@Component
class OrderCommandListener(
    private val processor: CommandMessageProcessor,
    private val deadLetterPublisher: CommandDeadLetterPublisher,
    private val notificationPort: NotificationPort,
) {
    @SqsListener(queueNames = ["\${mechanical-hub.orders.queues.orders-commands}"])
    fun onMessage(
        message: Message<String>,
        acknowledgement: Acknowledgement,
        visibility: Visibility,
    ) {
        val headers = message.headers
        LogContext.with(
            // MessageGroupId = orderId e MessageDeduplicationId = eventId; transactionId entra apos o parse.
            LogContext.ORDER_ID to headers[SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER],
            LogContext.EVENT_ID to headers[SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER],
            LogContext.TRANSACTION_ID to null,
        ) {
            handle(message, acknowledgement, visibility)
        }
    }

    private fun handle(
        message: Message<String>,
        acknowledgement: Acknowledgement,
        visibility: Visibility,
    ) {
        val receiveCount = message.headers[SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT]?.toString()?.toIntOrNull() ?: 1

        when (val decision = processor.process(message.payload, receiveCount)) {
            is CommandDecision.Acknowledge -> {
                acknowledgement.acknowledge()
                // Depois do COMMIT: falha no envio so gera log (notificationPort nao lanca).
                decision.notification?.let(notificationPort::send)
            }

            is CommandDecision.Retry -> {
                // O FAIL (WARN) da tentativa ja saiu no processador.
                visibility.changeTo(decision.delaySeconds)
            }

            is CommandDecision.DeadLetter -> {
                deadLetterPublisher.publish(message, decision.error)
                acknowledgement.acknowledge()
                FlowLog.write(
                    log,
                    LogLevel.ERROR,
                    FlowFields(
                        orderId = MDC.get(LogContext.ORDER_ID),
                        status = processor.statusOf(message.payload),
                        msg = "Comando enviado para a DLQ",
                        stage = Stage.FAIL,
                        attempt = receiveCount,
                        error = decision.error,
                    ),
                )
            }
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(OrderCommandListener::class.java)
    }
}
