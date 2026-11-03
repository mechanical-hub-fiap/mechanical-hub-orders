package com.fiap.mechanicalhub.orders.application.usecases.commands

import com.fiap.mechanicalhub.orders.application.commands.OrderCommand
import com.fiap.mechanicalhub.orders.application.ports.ServiceOrderRepository
import com.fiap.mechanicalhub.orders.application.usecases.order.requireById
import com.fiap.mechanicalhub.orders.logging.FlowLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** `markStockPending` → `has_stock_pending = true` (td_stock_pending.md P3). */
@Service
class MarkStockPendingUseCase(
    private val serviceOrderRepository: ServiceOrderRepository,
    private val runner: IdempotentCommandRunner,
) {
    @Transactional
    fun execute(command: OrderCommand.MarkStockPending): CommandOutcome =
        runner.run(command.idempotencyKey) {
            val order =
                serviceOrderRepository.save(serviceOrderRepository.requireById(command.orderId).markStockPending(command.transactionId))
            FlowLog.detail("orderStatus" to order.status, "hasStockPending" to order.hasStockPending)
            CommandEffect()
        }
}
