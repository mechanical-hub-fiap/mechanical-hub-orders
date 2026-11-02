package com.fiap.mechanicalhub.orders.infrastructure.http.controllers

import com.fiap.mechanicalhub.orders.application.ports.ServiceOrderFilter
import com.fiap.mechanicalhub.orders.application.usecases.order.AddRepairsUseCase
import com.fiap.mechanicalhub.orders.application.usecases.order.ChangeOrderStatusUseCase
import com.fiap.mechanicalhub.orders.application.usecases.order.OpenServiceOrderCommand
import com.fiap.mechanicalhub.orders.application.usecases.order.OpenServiceOrderUseCase
import com.fiap.mechanicalhub.orders.application.usecases.order.QueryServiceOrdersUseCase
import com.fiap.mechanicalhub.orders.application.usecases.order.UpdateTaskStatusUseCase
import com.fiap.mechanicalhub.orders.domain.enums.OrderStatusEnum
import com.fiap.mechanicalhub.orders.infrastructure.http.RouteStep
import com.fiap.mechanicalhub.orders.infrastructure.http.dto.AddRepairsRequest
import com.fiap.mechanicalhub.orders.infrastructure.http.dto.ChangeOrderStatusRequest
import com.fiap.mechanicalhub.orders.infrastructure.http.dto.OpenServiceOrderRequest
import com.fiap.mechanicalhub.orders.infrastructure.http.dto.ServiceOrderResponse
import com.fiap.mechanicalhub.orders.infrastructure.http.dto.UpdateTaskStatusRequest
import com.fiap.mechanicalhub.orders.infrastructure.http.dto.toResponse
import com.fiap.mechanicalhub.orders.infrastructure.http.restStep
import com.fiap.mechanicalhub.orders.infrastructure.security.GatewayPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/orders")
@Tag(name = "Ordens de servico", description = "Mecanico e administrador (protegido pelo API Gateway)")
class ServiceOrderController(
    private val openServiceOrder: OpenServiceOrderUseCase,
    private val addRepairs: AddRepairsUseCase,
    private val changeOrderStatus: ChangeOrderStatusUseCase,
    private val updateTaskStatus: UpdateTaskStatusUseCase,
    private val queryServiceOrders: QueryServiceOrdersUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre a ordem ja em diagnostico, com os reparos informados")
    fun open(
        @Valid @RequestBody request: OpenServiceOrderRequest,
        @AuthenticationPrincipal principal: GatewayPrincipal,
    ): ServiceOrderResponse {
        val order =
            restStep(log, null, CREATE_ORDER) {
                openServiceOrder.execute(
                    OpenServiceOrderCommand(
                        customerId = requireNotNull(request.customerId),
                        vehicleId = requireNotNull(request.vehicleId),
                        repairIds = request.serviceIds.orEmpty(),
                        requestDescription = request.requestDescription.orEmpty(),
                        userId = principal.id,
                    ),
                )
            }
        return queryServiceOrders.findById(order.id).toResponse()
    }

    @PatchMapping("/{orderId}/repairs")
    @Operation(summary = "Adiciona reparos (somente em IN_DIAGNOSIS)")
    fun addRepairs(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: AddRepairsRequest,
    ): ServiceOrderResponse {
        restStep(log, orderId, ADD_REPAIRS) {
            addRepairs.execute(orderId, request.repairs.orEmpty().mapNotNull { it.repairId })
        }
        return queryServiceOrders.findById(orderId).toResponse()
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Muda o status: PREPARING, IN_PROGRESS, FINISHED ou DELIVERED")
    fun changeStatus(
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: ChangeOrderStatusRequest,
    ): ServiceOrderResponse {
        val route = request.status?.let { STATUS_STEPS[it] } ?: CHANGE_ORDER_STATUS
        restStep(log, orderId, route) {
            changeOrderStatus.execute(orderId, requireNotNull(request.status))
        }
        return queryServiceOrders.findById(orderId).toResponse()
    }

    @PostMapping("/status")
    @Operation(summary = "Muda o status de um reparo (PENDING → STARTED → FINISHED) com a ordem em IN_PROGRESS")
    fun updateTaskStatus(
        @Valid @RequestBody request: UpdateTaskStatusRequest,
    ): ServiceOrderResponse {
        val orderId = requireNotNull(request.orderId)
        restStep(log, orderId, UPDATE_TASK_STATUS) {
            updateTaskStatus.execute(orderId, requireNotNull(request.repairId), requireNotNull(request.status))
        }
        return queryServiceOrders.findById(orderId).toResponse()
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Consulta a ordem (inclui has_stock_pending)")
    fun findById(
        @PathVariable orderId: UUID,
    ): ServiceOrderResponse = queryServiceOrders.findById(orderId).toResponse()

    @GetMapping
    @Operation(summary = "Lista ordens por status, cliente ou responsavel")
    fun search(
        @RequestParam(required = false) status: OrderStatusEnum?,
        @RequestParam(required = false) customerId: UUID?,
        @RequestParam(required = false) responsibleUserId: UUID?,
    ): List<ServiceOrderResponse> =
        queryServiceOrders
            .search(ServiceOrderFilter(status = status, customerId = customerId, responsibleUserId = responsibleUserId))
            .map { it.toResponse() }

    private companion object {
        val log = LoggerFactory.getLogger(ServiceOrderController::class.java)

        val CREATE_ORDER = RouteStep("CREATE_ORDER", "Abertura de ordem recebida", "Ordem aberta em diagnostico")
        val ADD_REPAIRS = RouteStep("ADD_REPAIRS", "Inclusao de reparos recebida", "Reparos adicionados a ordem")
        val UPDATE_TASK_STATUS =
            RouteStep("UPDATE_TASK_STATUS", "Mudanca de status do reparo recebida", "Status do reparo alterado")
        val CHANGE_ORDER_STATUS = RouteStep("CHANGE_ORDER_STATUS", "Mudanca de status recebida", "Status da ordem alterado")

        /** `status` do log pelo status pedido pelo mecanico (logging-standard §3). */
        val STATUS_STEPS =
            mapOf(
                OrderStatusEnum.PREPARING to
                    RouteStep(
                        "COMPLETE_DIAGNOSIS",
                        "Finalizacao do diagnostico recebida",
                        "Diagnostico finalizado, evento diagnosisCompleted gravado no outbox",
                    ),
                OrderStatusEnum.IN_PROGRESS to
                    RouteStep("START_EXECUTION", "Inicio da execucao recebido", "Execucao da ordem iniciada"),
                OrderStatusEnum.FINISHED to
                    RouteStep(
                        "FINISH_ORDER",
                        "Finalizacao da ordem recebida",
                        "Ordem finalizada, evento orderFinished gravado no outbox",
                    ),
                OrderStatusEnum.DELIVERED to
                    RouteStep("DELIVER_ORDER", "Entrega do veiculo recebida", "Veiculo entregue ao cliente"),
            )
    }
}
