package com.example.pocgeofencing.data

/**
 * Perímetros de exemplo sobre os trajetos de [RoadRoutes], cobrindo os três alvos
 * possíveis: a frota inteira, um grupo de veículos e um veículo específico.
 */
object SampleGeofences {

    /** Grupo dos veículos de carga maior (Sprinter e Accelo). */
    private val SPRINTER_GROUP = setOf("v2", "v3")

    val all = listOf(
        Geofence(
            id = "cd",
            name = "Centro de distribuição",
            latitude = SampleFleet.DISTRIBUTION_CENTER.first,
            longitude = SampleFleet.DISTRIBUTION_CENTER.second,
            radiusMeters = 250.0,
            vehicleIds = null // frota inteira
        ),
        Geofence(
            id = "cliente-fiorino",
            name = "Cliente Vila Xavier",
            latitude = RoadRoutes.fiorinoOut.last().first,
            longitude = RoadRoutes.fiorinoOut.last().second,
            radiusMeters = 200.0,
            vehicleIds = setOf("v1") // só a Fiorino
        ),
        Geofence(
            id = "base-sprinter",
            name = "Base Jardim Primavera",
            latitude = RoadRoutes.sprinterLeg1.first().first,
            longitude = RoadRoutes.sprinterLeg1.first().second,
            radiusMeters = 200.0,
            vehicleIds = SPRINTER_GROUP
        ),
        Geofence(
            id = "cliente-sprinter-1",
            name = "Cliente Centro",
            latitude = RoadRoutes.sprinterLeg1.last().first,
            longitude = RoadRoutes.sprinterLeg1.last().second,
            radiusMeters = 180.0,
            vehicleIds = SPRINTER_GROUP
        ),
        Geofence(
            id = "cliente-sprinter-2",
            name = "Cliente Jardim Brasil",
            latitude = RoadRoutes.sprinterLeg2.last().first,
            longitude = RoadRoutes.sprinterLeg2.last().second,
            radiusMeters = 180.0,
            vehicleIds = SPRINTER_GROUP
        )
    )

    fun targetLabel(fence: Geofence, vehicles: List<SimulatedVehicle>): String = when {
        fence.vehicleIds == null -> "Frota inteira"
        fence.vehicleIds.size == 1 ->
            vehicles.find { it.id in fence.vehicleIds }?.plate ?: "1 veículo"
        else -> "Grupo de ${fence.vehicleIds.size} veículos"
    }
}
