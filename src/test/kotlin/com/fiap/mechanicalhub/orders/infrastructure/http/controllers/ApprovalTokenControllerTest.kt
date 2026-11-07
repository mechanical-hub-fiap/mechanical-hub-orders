package com.fiap.mechanicalhub.orders.infrastructure.http.controllers

import com.fiap.mechanicalhub.orders.application.usecases.order.ReissueApprovalTokenUseCase
import com.fiap.mechanicalhub.orders.application.usecases.order.ReissuedApprovalToken
import com.fiap.mechanicalhub.orders.domain.approval.ApprovalToken
import com.fiap.mechanicalhub.orders.domain.enums.OrderStatusEnum
import com.fiap.mechanicalhub.orders.domain.exceptions.InvalidOrderTransitionException
import com.fiap.mechanicalhub.orders.fixtures.Seeds
import com.fiap.mechanicalhub.orders.infrastructure.config.OrdersProperties
import com.fiap.mechanicalhub.orders.infrastructure.security.GatewayPrincipal
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.security.SecureRandom
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApprovalTokenControllerTest {
    private val useCase = mockk<ReissueApprovalTokenUseCase>()
    private val properties = OrdersProperties(OrdersProperties.Queues("c", "d", "o"), "http://front/")
    private val controller = ApprovalTokenController(useCase, properties)
    private val admin = GatewayPrincipal(Seeds.MECHANIC, "Admin", "ADMINISTRATOR")

    @Test
    fun `given an order waiting approval when reissuing then it returns the token and the link without caching`() {
        val orderId = UUID.randomUUID()
        val token = ApprovalToken.generate(SecureRandom())
        every { useCase.execute(orderId, admin.id) } returns ReissuedApprovalToken(orderId, "OS-202610-0001", token)

        val response = controller.reissue(orderId, admin)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(token.value, response.body?.approvalToken)
        assertEquals("http://front/orders/$orderId/approval?token=${token.value}", response.body?.approvalLink)
        assertTrue(assertNotNull(response.headers.cacheControl).contains("no-store"))
    }

    @Test
    fun `given an order outside waiting approval when reissuing then the rejection propagates`() {
        val orderId = UUID.randomUUID()
        every { useCase.execute(orderId, admin.id) } throws InvalidOrderTransitionException(orderId, OrderStatusEnum.APPROVED, "reemitir")

        assertFailsWith<InvalidOrderTransitionException> { controller.reissue(orderId, admin) }
    }
}
