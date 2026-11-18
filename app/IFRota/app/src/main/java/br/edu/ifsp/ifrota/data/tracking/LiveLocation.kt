package br.edu.ifsp.ifrota.data.tracking

import br.edu.ifsp.ifrota.data.geofencing.GeofenceTransition

/** Em que ponto do dia o entregador está, como o administrador enxerga. */
enum class TrackingPhase {
    /** Sem entregas atribuídas ou sem veículo escolhido. */
    IDLE,
    /** Parado, com a rota até a próxima entrega pronta para começar. */
    PLANNING,
    NAVIGATING,
    PAUSED,
    /** No endereço da entrega, falta confirmar. */
    ARRIVED,
    /** Todas as entregas do dia concluídas. */
    FINISHED
}

/**
 * Última posição conhecida de um entregador, publicada pelo app dele em
 * `locations/{driverId}` (um documento por entregador, sobrescrito a cada envio).
 */
data class LiveLocation(
    val driverId: String,
    val plate: String,
    val latitude: Double,
    val longitude: Double,
    val heading: Float,
    val speedKmh: Int,
    val phase: TrackingPhase,
    val deliveryId: String? = null,
    val deliveryOrder: Int? = null,
    val deliveryRecipient: String? = null,
    val deliveryLatitude: Double? = null,
    val deliveryLongitude: Double? = null,
    val doneCount: Int = 0,
    val totalDeliveries: Int = 0,
    val updatedAt: Long,
    /** O entregador saiu da conta: a última posição fica no mapa do admin, como offline. */
    val signedOut: Boolean = false
) {
    /** Saiu da conta, ou sem notícias há mais de [OFFLINE_AFTER_MILLIS] (app fechado ou sem conexão). */
    fun isOnline(now: Long): Boolean = !signedOut && now - updatedAt <= OFFLINE_AFTER_MILLIS

    /** Mudanças que o administrador precisa ver na hora, sem esperar o intervalo de envio. */
    fun milestoneDiffers(other: LiveLocation): Boolean =
        phase != other.phase ||
            deliveryId != other.deliveryId ||
            plate != other.plate ||
            doneCount != other.doneCount ||
            totalDeliveries != other.totalDeliveries

    fun samePosition(other: LiveLocation): Boolean =
        latitude == other.latitude && longitude == other.longitude

    companion object {
        /** O app do entregador manda ao menos um sinal por minuto; 2 min sem nada = offline. */
        const val OFFLINE_AFTER_MILLIS = 2 * 60_000L
    }
}

/** Entrada ou saída de perímetro registrada pelo app do entregador em `geofenceEvents`. */
data class FenceEventRecord(
    val id: String,
    val driverId: String,
    val plate: String,
    val fenceId: String,
    val fenceName: String,
    val transition: GeofenceTransition,
    val timestampMillis: Long
)
