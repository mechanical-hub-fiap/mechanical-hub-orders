package com.fiap.mechanicalhub.orders.infrastructure.http

import com.fiap.mechanicalhub.orders.domain.exceptions.DomainException
import com.fiap.mechanicalhub.orders.logging.FlowLog
import com.fiap.mechanicalhub.orders.logging.FlowStep
import org.slf4j.Logger
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.dao.TransientDataAccessException

/**
 * Executa a rota como uma etapa do log (logging-standard §4): `START` na entrada HTTP, `END` com o contexto que o
 * caso de uso acrescentou e `FAIL` quando a requisicao falha. A rota decide a resposta HTTP, entao e ela quem loga o
 * `FAIL`; o `GlobalExceptionHandler` so traduz a excecao.
 */
@Suppress("TooGenericExceptionCaught")
fun <T> restStep(
    log: Logger,
    orderId: Any?,
    route: RouteStep,
    block: () -> T,
): T {
    val step = FlowLog.begin(log, orderId, route.status)
    step.start(route.start)
    return try {
        block().also { step.end(route.end) }
    } catch (error: Exception) {
        logFailure(step, error)
        throw error
    }
}

/**
 * Rejeicao de regra ou de contrato (404, 409, 422) e falha transitoria (503, o cliente repete) nao exigem
 * intervencao: WARN. Qualquer outra falha e inesperada: ERROR com stack trace.
 */
private fun logFailure(
    step: FlowStep,
    error: Exception,
) {
    when (error) {
        is DomainException, is IllegalArgumentException -> {
            step.warnFail("Requisicao rejeitada pela regra da ordem", error)
        }

        is OptimisticLockingFailureException -> {
            step.warnFail("Conflito de versao, a ordem foi alterada por outra requisicao", error)
        }

        is TransientDataAccessException -> {
            step.warnFail("Falha transitoria de banco, o cliente pode repetir", error)
        }

        else -> {
            step.errorFail("Falha inesperada na requisicao", error)
        }
    }
}
