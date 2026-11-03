package com.fiap.mechanicalhub.orders.infrastructure.http.dto

import com.fiap.mechanicalhub.orders.application.usecases.customer.CustomerOrderView
import com.fiap.mechanicalhub.orders.application.usecases.order.ServiceOrderDetails
import com.fiap.mechanicalhub.orders.domain.enums.OrderStatusEnum
import com.fiap.mechanicalhub.orders.domain.enums.TaskStatusEnum
import com.fiap.mechanicalhub.orders.domain.model.Repair

fun OrderStatusEnum.toResponse() = StatusResponse(name, displayName)

fun TaskStatusEnum.toResponse() = StatusResponse(name, displayName)

fun ServiceOrderDetails.toResponse(): ServiceOrderResponse =
    ServiceOrderResponse(
        id = order.id,
        orderNumber = order.orderNumber,
        status = order.status.toResponse(),
        customerId = order.customerId,
        vehicleId = order.vehicleId,
        createdByUserId = order.createdByUserId,
        responsibleUserId = order.responsibleUserId,
        requestDescription = order.requestDescription,
        budget = order.budget,
        hasStockPending = order.hasStockPending,
        repairs =
            order.tasks.map {
                OrderTaskResponse(it.repairId, repairNames[it.repairId], it.status.toResponse(), it.startedAt, it.finishedAt)
            },
        openedAt = order.openedAt,
        completedAt = order.completedAt,
        deliveredAt = order.deliveredAt,
    )

fun Repair.toResponse() = RepairResponse(id = id, name = name, description = description)

fun CustomerOrderView.toResponse() =
    CustomerOrderResponse(
        orderId = orderId,
        orderNumber = orderNumber,
        customerName = customerName,
        vehicleLicensePlate = vehicleLicensePlate,
        vehicleModel = vehicleModel,
        vehicleBrand = vehicleBrand,
        status = status.toResponse(),
        budget = budget,
        services = services,
        openedAt = openedAt,
        completedAt = completedAt,
        deliveredAt = deliveredAt,
    )
