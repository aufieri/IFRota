package br.edu.ifsp.ifrota.data.geofencing

import br.edu.ifsp.ifrota.data.delivery.Delivery
import br.edu.ifsp.ifrota.data.delivery.DistributionCenter

/**
 * Perímetros monitorados durante a rota do motorista:
 * - o centro de distribuição, sempre (saída e retorno do veículo);
 * - a parada de onde o veículo saiu (a entrega anterior), para avisar quando ele deixa o local;
 * - a entrega atual, para avisar quando ele chega perto do endereço.
 *
 * As demais entregas da fila ficam de fora: passar perto de um endereço já atendido, ou de
 * um que ainda não é o próximo, só geraria ruído.
 */
object DeliveryGeofences {
    const val CD_ID = "cd"
    const val CD_RADIUS_METERS = 200.0
    const val DELIVERY_RADIUS_METERS = 150.0

    val distributionCenter = Geofence(
        id = CD_ID,
        name = "${DistributionCenter.NAME} (CD)",
        latitude = DistributionCenter.LATITUDE,
        longitude = DistributionCenter.LONGITUDE,
        radiusMeters = CD_RADIUS_METERS
    )

    fun idOf(delivery: Delivery) = "entrega-${delivery.id}"

    fun of(delivery: Delivery) = Geofence(
        id = idOf(delivery),
        name = "Entrega #${delivery.order} · ${delivery.recipient}",
        latitude = delivery.latitude,
        longitude = delivery.longitude,
        radiusMeters = DELIVERY_RADIUS_METERS
    )

    /** [current] é o índice da entrega atual na fila (igual a deliveries.size com tudo concluído). */
    fun forRoute(deliveries: List<Delivery>, current: Int): List<Geofence> =
        listOf(distributionCenter) + listOfNotNull(
            deliveries.getOrNull(current - 1),
            deliveries.getOrNull(current)
        )
            .filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
            .map(::of)
}
