package com.fiap.mechanicalhub.orders.application.usecases.order

import com.fiap.mechanicalhub.orders.application.usecases.customer.ApproveOrderUseCase
import com.fiap.mechanicalhub.orders.domain.approval.ApprovalToken
import com.fiap.mechanicalhub.orders.domain.enums.OrderStatusEnum
import com.fiap.mechanicalhub.orders.domain.exceptions.InvalidOrderTransitionException
import com.fiap.mechanicalhub.orders.domain.exceptions.ServiceOrderNotFoundException
import com.fiap.mechanicalhub.orders.fixtures.FakeOutbox
import com.fiap.mechanicalhub.orders.fixtures.FakeServiceOrderRepository
import com.fiap.mechanicalhub.orders.fixtures.aServiceOrder
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.security.SecureRandom
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ReissueApprovalTokenUseCaseTest {
    private val oldToken = ApprovalToken.generate(SecureRandom())
    private val waiting =
        aServiceOrder(status = OrderStatusEnum.WAITING_APPROVAL, sagaTransactionId = UUID.randomUUID(), approvalTokenHash = oldToken.hash())
            .copy(budget = BigDecimal("308.10"))
    private val orders = FakeServiceOrderRepository(waiting)
    private val useCase = ReissueApprovalTokenUseCase(orders, SecureRandom())

    @Test
    fun `given an order waiting approval when reissuing then the new token approves it and the old one does not`() {
        val reissued = useCase.execute(waiting.id, UUID.randomUUID())

        assertNotEquals(oldToken.value, reissued.token.value)
        assertEquals(reissued.token.hash(), orders.findById(waiting.id)?.approvalTokenHash)
        val approve = ApproveOrderUseCase(orders, FakeOutbox())
        assertFailsWith<ServiceOrderNotFoundException> { approve.execute(waiting.id, oldToken.value) }
        approve.execute(waiting.id, reissued.token.value)
        assertEquals(OrderStatusEnum.APPROVED, orders.findById(waiting.id)?.status)
    }

    @Test
    fun `given an order outside waiting approval when reissuing then it is an invalid transition`() {
        val approved = waiting.copy(status = OrderStatusEnum.APPROVED)

        assertFailsWith<InvalidOrderTransitionException> {
            ReissueApprovalTokenUseCase(FakeServiceOrderRepository(approved), SecureRandom()).execute(approved.id, UUID.randomUUID())
        }
    }

    @Test
    fun `given an unknown order when reissuing then it is not found`() {
        assertFailsWith<ServiceOrderNotFoundException> { useCase.execute(UUID.randomUUID(), UUID.randomUUID()) }
    }
}
