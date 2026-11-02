package com.fiap.mechanicalhub.orders.infrastructure.messaging.outbox

import com.fiap.mechanicalhub.orders.infrastructure.observability.LogContext
import com.fiap.mechanicalhub.orders.logging.FlowFields
import com.fiap.mechanicalhub.orders.logging.FlowLog
import com.fiap.mechanicalhub.orders.logging.LogLevel
import com.fiap.mechanicalhub.orders.logging.Stage
import org.slf4j.LoggerFactory
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.util.UUID

/** Linha `PENDING` do outbox pronta para envio. */
data class PendingOutboxMessage(
    val sequence: Long,
    val eventId: UUID,
    val aggregateId: UUID,
    val eventType: String,
    val payload: String,
)

/** Acesso ao `outbox_events` usado pelo relay. */
interface OutboxStore {
    /** Lideranca do ciclo: so a instancia que obtem o lock publica. */
    fun tryAcquireLeadership(): Boolean

    fun findPending(limit: Int): List<PendingOutboxMessage>

    fun markSent(
        sequence: Long,
        clock: Clock,
    )

    fun registerFailure(
        sequence: Long,
        error: String,
    )
}

/** Envio sincrono para a fila FIFO do orquestrador. */
fun interface OutboxMessagePublisher {
    fun publish(message: PendingOutboxMessage)
}

/**
 * Relay do transactional outbox (failure_handling.md §4): publica os eventos
 * `PENDING` em ordem de `sequence`, um por vez, avancando so apos a confirmacao
 * do SQS. Uma falha interrompe o lote (sem pular a linha, preservando a ordem)
 * e incrementa `attempts`; o proximo ciclo tenta de novo com o mesmo `eventId`.
 */
class OutboxRelay(
    private val store: OutboxStore,
    private val publisher: OutboxMessagePublisher,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
    private val batchSize: Int,
) {
    /** Retorna quantas mensagens foram publicadas neste ciclo. */
    fun relayBatch(): Int = transactionTemplate.execute { relayInsideTransaction() }

    private fun relayInsideTransaction(): Int {
        if (!store.tryAcquireLeadership()) return 0

        var published = 0
        for (message in store.findPending(batchSize)) {
            val sent =
                LogContext.with(
                    LogContext.ORDER_ID to message.aggregateId,
                    LogContext.EVENT_ID to message.eventId,
                ) { publish(message) }
            if (!sent) break
            published++
        }
        return published
    }

    private fun publish(message: PendingOutboxMessage): Boolean {
        val failure = runCatching { publisher.publish(message) }.exceptionOrNull()
        if (failure != null) {
            logPublication(message, "Falha ao publicar evento ${message.eventType}, nova tentativa no proximo ciclo", failure)
            store.registerFailure(message.sequence, failure.toString())
            return false
        }
        store.markSent(message.sequence, clock)
        logPublication(message, "Evento ${message.eventType} publicado na fila do orquestrador", null)
        return true
    }

    /** Publicacao pelo relay (logging-standard §4), com o `status` da etapa que gravou o evento. */
    private fun logPublication(
        message: PendingOutboxMessage,
        msg: String,
        failure: Throwable?,
    ) {
        val fields =
            FlowFields(
                orderId = message.aggregateId,
                status = EVENT_STATUS[message.eventType],
                msg = msg,
                stage = if (failure != null) Stage.FAIL else null,
                context = listOf("eventType" to message.eventType),
                error = failure,
            )
        FlowLog.write(log, if (failure != null) LogLevel.WARN else LogLevel.INFO, fields)
    }

    private companion object {
        val log = LoggerFactory.getLogger(OutboxRelay::class.java)
        val EVENT_STATUS =
            mapOf(
                "diagnosisCompleted" to "COMPLETE_DIAGNOSIS",
                "orderApproved" to "APPROVE_ORDER",
                "orderRejected" to "REJECT_ORDER",
                "orderFinished" to "FINISH_ORDER",
                "orderCanceled" to "CANCEL_ORDER",
            )
    }
}
