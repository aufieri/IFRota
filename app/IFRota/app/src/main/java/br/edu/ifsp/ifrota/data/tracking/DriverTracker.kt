package br.edu.ifsp.ifrota.data.tracking

import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryPhase
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryUiState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Publica a posição do entregador para o administrador, sem gravar a cada tick da simulação
 * (1,5 s: ~2.400 gravações por hora, o que esgotaria rápido a cota gratuita do Firestore).
 * Ver [shouldPublish] para a regra.
 */
class DriverTracker(
    private val driverId: String,
    private val repository: TrackingRepository,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private var lastSent: LiveLocation? = null

    /**
     * Acompanha o andamento até ser cancelado. Além de reagir a cada mudança de estado, verifica
     * periodicamente, para mandar o sinal de "ainda online" com o veículo parado.
     */
    suspend fun run(states: StateFlow<DeliveryUiState>) = coroutineScope {
        launch { states.collect(::check) }
        launch {
            while (true) {
                delay(CHECK_INTERVAL_MILLIS)
                check(states.value)
            }
        }
    }

    private fun check(state: DeliveryUiState) {
        if (!state.loaded) return
        val location = state.toLiveLocation(driverId, clock())
        if (shouldPublish(location, lastSent)) {
            lastSent = location
            repository.publishLocation(location)
        }
    }

    companion object {
        /** Em movimento, no máximo um envio a cada 5 s; o mapa do admin desliza entre eles. */
        const val MOVING_INTERVAL_MILLIS = 5_000L

        /** Parado, um sinal por minuto basta para o admin saber que o app está aberto. */
        const val HEARTBEAT_MILLIS = 60_000L

        private const val CHECK_INTERVAL_MILLIS = 5_000L

        /**
         * Envia na hora quando muda algo que o admin precisa ver (fase, entrega atual, veículo,
         * progresso); em movimento, a cada [MOVING_INTERVAL_MILLIS]; parado, a cada [HEARTBEAT_MILLIS].
         */
        fun shouldPublish(new: LiveLocation, last: LiveLocation?): Boolean {
            if (last == null || new.milestoneDiffers(last)) return true
            val elapsed = new.updatedAt - last.updatedAt
            if (!new.samePosition(last) && elapsed >= MOVING_INTERVAL_MILLIS) return true
            return elapsed >= HEARTBEAT_MILLIS
        }
    }
}

/** Retrato do andamento do entregador no formato publicado para o administrador. */
fun DeliveryUiState.toLiveLocation(driverId: String, now: Long): LiveLocation {
    val phase = when {
        !hasDeliveries || vehicle == null -> TrackingPhase.IDLE
        allDone -> TrackingPhase.FINISHED
        else -> when (this.phase) {
            DeliveryPhase.PLANNING -> TrackingPhase.PLANNING
            DeliveryPhase.NAVIGATING -> TrackingPhase.NAVIGATING
            DeliveryPhase.PAUSED -> TrackingPhase.PAUSED
            DeliveryPhase.ARRIVED -> TrackingPhase.ARRIVED
        }
    }
    val target = currentDelivery.takeUnless { allDone }
    val point = telemetry
    return LiveLocation(
        driverId = driverId,
        plate = vehicle?.plate.orEmpty(),
        latitude = point.latitude,
        longitude = point.longitude,
        heading = heading,
        speedKmh = point.speedKmh,
        phase = phase,
        deliveryId = target?.id,
        deliveryOrder = target?.order,
        deliveryRecipient = target?.recipient,
        deliveryLatitude = target?.latitude,
        deliveryLongitude = target?.longitude,
        doneCount = doneCount,
        totalDeliveries = deliveries.size,
        updatedAt = now
    )
}
