package com.fiap.mechanicalhub.orders.application.usecases.customer

import com.fiap.mechanicalhub.orders.application.ports.CustomerRepository
import com.fiap.mechanicalhub.orders.application.ports.RepairRepository
import com.fiap.mechanicalhub.orders.application.ports.ServiceOrderRepository
import com.fiap.mechanicalhub.orders.application.ports.VehicleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** GET /mechanical-hub/service-orders/{orderNumber}. */
@Service
class GetCustomerOrderUseCase(
    private val serviceOrderRepository: ServiceOrderRepository,
    private val customerRepository: CustomerRepository,
    private val vehicleRepository: VehicleRepository,
    private val repairRepository: RepairRepository,
) {
    @Transactional(readOnly = true)
    fun execute(
        orderNumber: String,
        approvalToken: String?,
    ): CustomerOrderView {
        val order = serviceOrderRepository.findByOrderNumber(orderNumber).authorizedBy(approvalToken, orderNumber)
        val customer = customerRepository.findById(order.customerId)
        val vehicle = vehicleRepository.findById(order.vehicleId)
        return CustomerOrderView(
            orderId = order.id,
            orderNumber = order.orderNumber,
            customerName = customer?.name.orEmpty(),
            vehicleLicensePlate = vehicle?.licensePlate.orEmpty(),
            vehicleBrand = vehicle?.brand.orEmpty(),
            vehicleModel = vehicle?.model.orEmpty(),
            status = order.status,
            budget = order.budget,
            services = repairRepository.findAllByIds(order.repairIds).map { it.name },
            openedAt = order.openedAt,
            completedAt = order.completedAt,
            deliveredAt = order.deliveredAt,
        )
    }
}
