package com.fiap.mechanicalhub.orders.infrastructure.http.controllers

import com.fiap.mechanicalhub.orders.application.usecases.order.ReissueApprovalTokenUseCase
import com.fiap.mechanicalhub.orders.application.usecases.order.ReissuedApprovalToken
import com.fiap.mechanicalhub.orders.domain.approval.ApprovalToken
import com.fiap.mechanicalhub.orders.fixtures.Seeds
import com.fiap.mechanicalhub.orders.infrastructure.http.error.GlobalExceptionHandler
import com.fiap.mechanicalhub.orders.infrastructure.security.GatewayAuthenticationFilter
import com.fiap.mechanicalhub.orders.infrastructure.security.SecurityConfig
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import com.fiap.mechanicalhub.orders.infrastructure.config.OrdersProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.post
import java.security.SecureRandom
import java.util.UUID

/** Rota ligada pela flag: so ADMINISTRATOR passa pelo SecurityConfig real. Sem banco nem Docker. */
@WebMvcTest(ApprovalTokenController::class)
@Import(SecurityConfig::class, GatewayAuthenticationFilter::class, GlobalExceptionHandler::class, ApprovalTokenSecurityTest.Beans::class)
@TestPropertySource(
    properties = [
        "mechanical-hub.orders.approval-token-reissue-enabled=true",
        "mechanical-hub.orders.queues.orders-commands=c",
        "mechanical-hub.orders.queues.orders-commands-dlq=d",
        "mechanical-hub.orders.queues.orchestrator-events=o",
        "mechanical-hub.orders.frontend-base-url=http://front",
    ],
)
class ApprovalTokenSecurityTest {
    @TestConfiguration
    @EnableConfigurationProperties(OrdersProperties::class)
    class Beans {
        @Bean
        fun reissueApprovalTokenUseCase(): ReissueApprovalTokenUseCase = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var useCase: ReissueApprovalTokenUseCase

    private val orderId = UUID.randomUUID()

    @BeforeEach
    fun resetMock() {
        clearMocks(useCase)
        every { useCase.execute(any(), any()) } returns
            ReissuedApprovalToken(orderId, "OS-202610-0001", ApprovalToken.generate(SecureRandom()))
    }

    @Test
    fun `given an administrator when reissuing then it is ok and the token is not cacheable`() {
        mockMvc
            .post("/orders/$orderId/approval-token") { asRole("ADMINISTRATOR") }
            .andExpect {
                status { isOk() }
                header { string("Cache-Control", "no-store") }
            }
        verify(exactly = 1) { useCase.execute(orderId, Seeds.MECHANIC) }
    }

    @Test
    fun `given a mechanic when reissuing then it is forbidden and nothing is reissued`() {
        mockMvc.post("/orders/$orderId/approval-token") { asRole("MECHANICAL") }.andExpect { status { isForbidden() } }
        verify(exactly = 0) { useCase.execute(any(), any()) }
    }

    @Test
    fun `given no identity when reissuing then it is unauthorized and nothing is reissued`() {
        mockMvc.post("/orders/$orderId/approval-token").andExpect { status { isUnauthorized() } }
        verify(exactly = 0) { useCase.execute(any(), any()) }
    }

    private fun MockHttpServletRequestDsl.asRole(role: String) {
        header("x-user-id", Seeds.MECHANIC.toString())
        header("x-user-role", role)
        header("x-user-name", "Teste")
    }
}
