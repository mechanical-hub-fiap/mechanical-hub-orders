package com.fiap.mechanicalhub.orders.application.usecases.commands

import com.fiap.mechanicalhub.orders.application.commands.OrderCommand
import com.fiap.mechanicalhub.orders.application.ports.ServiceOrderRepository
import com.fiap.mechanicalhub.orders.application.usecases.order.requireById
import com.fiap.mechanicalhub.orders.logging.FlowLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** `clearStockPending` → `has_stock_pending = false` (td_stock_pending.md P6). */
@Service
class ClearStockPendingUseCase(
    private val serviceOrderRepository: ServiceOrderRepository,
    private val runner: IdempotentCommandRunner,
) {
    @Transactional
    fun execute(command: OrderCommand.ClearStockPending): CommandOutcome =
        runner.run(command.idempotencyKey) {
            val order = serviceOrderRepository.save(serviceOrderRepository.requireById(command.orderId).clearStockPending())
            FlowLog.detail("orderStatus" to order.status, "hasStockPending" to order.hasStockPending)
            CommandEffect()
        }
}
