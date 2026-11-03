package com.fiap.mechanicalhub.orders.domain.exceptions

import java.util.UUID

class VehicleNotFoundException(
    vehicleId: UUID,
) : NotFoundException("Veiculo $vehicleId nao encontrado")
