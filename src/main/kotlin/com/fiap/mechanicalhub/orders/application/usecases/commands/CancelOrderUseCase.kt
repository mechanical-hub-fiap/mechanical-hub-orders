package com.fiap.mechanicalhub.orders.application.usecases.commands

import com.fiap.mechanicalhub.orders.application.commands.OrderCommand
import com.fiap.mechanicalhub.orders.application.events.OutgoingEvent
import com.fiap.mechanicalhub.orders.application.ports.ServiceOrderRepository
import com.fiap.mechanicalhub.orders.application.usecases.order.requireById
import com.fiap.mechanicalhub.orders.logging.FlowLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** `cancelOrder` → `CANCELED` + `orderCanceled` (TD_paymentFailed.md F5). Sem notificacao ao cliente. */
@Service
class CancelOrderUseCase(
    private val serviceOrderRepository: ServiceOrderRepository,
    private val runner: IdempotentCommandRunner,
) {
    @Transactional
    fun execute(command: OrderCommand.CancelOrder): CommandOutcome =
        runner.run(command.idempotencyKey) {
            val order = serviceOrderRepository.save(serviceOrderRepository.requireById(command.orderId).cancel())
            FlowLog.detail("orderStatus" to order.status, "reason" to command.reason)
            CommandEffect(response = OutgoingEvent.OrderCanceled(command.transactionId, order.id))
        }
}
