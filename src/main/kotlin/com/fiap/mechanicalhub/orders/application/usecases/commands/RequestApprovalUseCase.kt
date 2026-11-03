package com.fiap.mechanicalhub.orders.application.usecases.commands

import com.fiap.mechanicalhub.orders.application.commands.OrderCommand
import com.fiap.mechanicalhub.orders.application.notifications.Notification
import com.fiap.mechanicalhub.orders.application.ports.ServiceOrderRepository
import com.fiap.mechanicalhub.orders.application.usecases.order.requireById
import com.fiap.mechanicalhub.orders.domain.approval.ApprovalToken
import com.fiap.mechanicalhub.orders.logging.FlowLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

/** `sendApproval` → `WAITING_APPROVAL`; token novo so no primeiro processamento (TD_finishedOS.md 11). */
@Service
class RequestApprovalUseCase(
    private val serviceOrderRepository: ServiceOrderRepository,
    private val runner: IdempotentCommandRunner,
    private val secureRandom: SecureRandom,
) {
    @Transactional
    fun execute(command: OrderCommand.SendApproval): CommandOutcome =
        runner.run(command.idempotencyKey) {
            val token = ApprovalToken.generate(secureRandom)
            val order =
                serviceOrderRepository.save(
                    serviceOrderRepository
                        .requireById(command.orderId)
                        .requestApproval(command.transactionId, command.orderCost, token.hash()),
                )
            FlowLog.detail("orderStatus" to order.status, "budgetId" to command.budgetId, "orderCost" to command.orderCost)
            CommandEffect(
                notification = Notification.BudgetReady(order.id, order.orderNumber, order.customerId, command.orderCost, token),
            )
        }
}
