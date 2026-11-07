package com.fiap.mechanicalhub.orders.infrastructure.http.controllers

import com.fiap.mechanicalhub.orders.application.usecases.order.ReissueApprovalTokenUseCase
import com.fiap.mechanicalhub.orders.fixtures.Seeds
import com.fiap.mechanicalhub.orders.infrastructure.http.error.GlobalExceptionHandler
import com.fiap.mechanicalhub.orders.infrastructure.security.GatewayAuthenticationFilter
import com.fiap.mechanicalhub.orders.infrastructure.security.SecurityConfig
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.assertTrue

/** Flag desligada (ou ausente): o controller nao e criado e a rota nao existe, nem para o administrador. */
@WebMvcTest(ApprovalTokenController::class)
@Import(SecurityConfig::class, GatewayAuthenticationFilter::class, GlobalExceptionHandler::class, ApprovalTokenFlagDisabledTest.Beans::class)
@TestPropertySource(
    properties = [
        "mechanical-hub.orders.approval-token-reissue-enabled=false",
        "mechanical-hub.orders.queues.orders-commands=c",
        "mechanical-hub.orders.queues.orders-commands-dlq=d",
        "mechanical-hub.orders.queues.orchestrator-events=o",
        "mechanical-hub.orders.frontend-base-url=http://front",
    ],
)
class ApprovalTokenFlagDisabledTest {
    @TestConfiguration
    class Beans {
        @Bean
        fun reissueApprovalTokenUseCase(): ReissueApprovalTokenUseCase = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `given the flag off when the context starts then the controller is not registered`() {
        assertTrue(context.getBeanNamesForType(ApprovalTokenController::class.java).isEmpty())
    }

    @Test
    fun `given the flag off when an administrator calls the route then it is not found`() {
        mockMvc
            .post("/orders/${UUID.randomUUID()}/approval-token") {
                header("x-user-id", Seeds.MECHANIC.toString())
                header("x-user-role", "ADMINISTRATOR")
            }.andExpect { status { isNotFound() } }
    }
}
