package com.fiap.mechanicalhub.orders.infrastructure.messaging.commands

import com.fiap.mechanicalhub.orders.application.usecases.commands.CommandOutcome
import com.fiap.mechanicalhub.orders.infrastructure.observability.LogContext
import com.fiap.mechanicalhub.orders.logging.FlowLog
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component

/**
 * Parse → caso de uso → decisao, isolado do SDK para ser testado sem SQS.
 * Backoff bloqueante por `ApproximateReceiveCount`: 1 s, 2 s, 4 s, 8 s, 8 s
 * (failure_handling.md §2.2); o 7o recebimento vai sozinho para a DLQ.
 * Cada comando e uma etapa do log (logging-standard §4): `START` antes do caso
 * de uso, `END` depois do COMMIT e `FAIL` com WARN (vai ser reprocessado) ou
 * ERROR (vai para a DLQ).
 */
@Component
class CommandMessageProcessor(
    private val parser: OrderCommandParser,
    private val dispatcher: OrderCommandDispatcher,
) {
    fun process(
        body: String,
        receiveCount: Int,
    ): CommandDecision {
        val status = parser.statusOrNull(body)
        val orderId = parser.fieldOrNull(body, LogContext.ORDER_ID) ?: MDC.get(LogContext.ORDER_ID)
        val step = FlowLog.begin(log, orderId, status, receiveCount)
        step.start(status?.let { START_MESSAGES[it] } ?: "Iniciando comando")
        return runCatching {
            val command = parser.parse(body)
            LogContext.put(LogContext.TRANSACTION_ID, command.transactionId)
            dispatcher.dispatch(command)
        }.fold(
            onSuccess = { outcome ->
                when (outcome) {
                    is CommandOutcome.Applied -> {
                        step.end(status?.let { END_MESSAGES[it] } ?: "Comando processado")
                        CommandDecision.Acknowledge(outcome.notification)
                    }

                    CommandOutcome.Duplicate -> {
                        // Nao e erro (failure_handling §1): nada e reaplicado.
                        step.end("Comando duplicado ignorado, ja processado (inbox_events)", "outcome" to "DUPLICATE")
                        CommandDecision.Acknowledge(notification = null)
                    }
                }
            },
            onFailure = { error ->
                when (CommandErrorClassifier.classify(error)) {
                    ErrorClass.TRANSIENT -> {
                        val delay = backoffSeconds(receiveCount)
                        step.warnFail("Falha transitoria no comando, sera reprocessado", error, "retryInSeconds" to delay)
                        CommandDecision.Retry(delay, error)
                    }

                    ErrorClass.PERMANENT -> {
                        step.errorFail("Falha permanente no comando, sera enviado para a DLQ", error)
                        CommandDecision.DeadLetter(error)
                    }
                }
            },
        )
    }

    /** `status` do log do comando, para a linha do envio a DLQ. */
    fun statusOf(body: String): String? = parser.statusOrNull(body)

    private companion object {
        val BACKOFF_SECONDS = listOf(1, 2, 4, 8, 8)
        val log = LoggerFactory.getLogger(CommandMessageProcessor::class.java)
        val START_MESSAGES =
            mapOf(
                "SEND_APPROVAL" to "Orcamento recebido, solicitando aprovacao do cliente",
                "MARK_STOCK_PENDING" to "Pendencia de estoque recebida, marcando a ordem",
                "CLEAR_STOCK_PENDING" to "Estoque reservado, removendo a pendencia da ordem",
                "SEND_PAYMENT_LINK" to "Link de pagamento recebido, aguardando pagamento",
                "RELEASE_ORDER" to "Pagamento confirmado, liberando a ordem para execucao",
                "CANCEL_ORDER" to "Pagamento nao confirmado, cancelando a ordem (compensacao)",
            )
        val END_MESSAGES =
            mapOf(
                "SEND_APPROVAL" to "Ordem aguardando aprovacao do cliente",
                "MARK_STOCK_PENDING" to "Ordem marcada com pendencia de estoque",
                "CLEAR_STOCK_PENDING" to "Pendencia de estoque removida da ordem",
                "SEND_PAYMENT_LINK" to "Ordem aguardando pagamento",
                "RELEASE_ORDER" to "Ordem liberada para execucao",
                "CANCEL_ORDER" to "Ordem cancelada, evento orderCanceled gravado no outbox",
            )

        fun backoffSeconds(receiveCount: Int): Int = BACKOFF_SECONDS[(receiveCount - 1).coerceIn(BACKOFF_SECONDS.indices)]
    }
}
