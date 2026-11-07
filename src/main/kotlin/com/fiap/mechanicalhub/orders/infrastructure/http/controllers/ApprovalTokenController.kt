package com.fiap.mechanicalhub.orders.infrastructure.http.controllers

import com.fiap.mechanicalhub.orders.application.usecases.order.ReissueApprovalTokenUseCase
import com.fiap.mechanicalhub.orders.infrastructure.config.OrdersProperties
import com.fiap.mechanicalhub.orders.infrastructure.http.RouteStep
import com.fiap.mechanicalhub.orders.infrastructure.http.dto.ApprovalTokenResponse
import com.fiap.mechanicalhub.orders.infrastructure.http.restStep
import com.fiap.mechanicalhub.orders.infrastructure.notification.NotificationMessages
import com.fiap.mechanicalhub.orders.infrastructure.security.GatewayPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Reemissao do token de aprovacao, para demonstracoes e suporte: o token original nao e recuperavel (so o hash e
 * salvo), entao o endpoint gera outro e invalida o anterior. So existe com `mechanical-hub.orders.approval-token-reissue-enabled=true`
 * (variavel `APPROVAL_TOKEN_REISSUE_ENABLED`, ligada por padrao; com `false`, a rota nao e registrada). So ADMINISTRATOR.
 */
@RestController
@RequestMapping("/orders")
@Tag(name = "Ordens de servico")
@ConditionalOnProperty(prefix = "mechanical-hub.orders", name = ["approval-token-reissue-enabled"], havingValue = "true")
class ApprovalTokenController(
    private val reissueApprovalToken: ReissueApprovalTokenUseCase,
    properties: OrdersProperties,
) {
    private val messages = NotificationMessages(properties.frontendBaseUrl)

    @PostMapping("/{orderId}/approval-token")
    @Operation(summary = "Gera um novo token de aprovacao (invalida o anterior; somente em WAITING_APPROVAL)")
    fun reissue(
        @PathVariable orderId: UUID,
        @AuthenticationPrincipal principal: GatewayPrincipal,
    ): ResponseEntity<ApprovalTokenResponse> {
        val reissued = restStep(log, orderId, REISSUE_APPROVAL_TOKEN) { reissueApprovalToken.execute(orderId, principal.id) }
        val body =
            ApprovalTokenResponse(
                orderId = reissued.orderId,
                orderNumber = reissued.orderNumber,
                approvalToken = reissued.token.value,
                approvalLink = messages.approvalLink(reissued.orderId, reissued.token.value),
            )
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body)
    }

    private companion object {
        val log = LoggerFactory.getLogger(ApprovalTokenController::class.java)
        val REISSUE_APPROVAL_TOKEN =
            RouteStep(
                "REISSUE_APPROVAL_TOKEN",
                "Reemissao do token de aprovacao recebida",
                "Token de aprovacao reemitido, o anterior deixou de valer",
            )
    }
}
