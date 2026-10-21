package com.example.pocmapa.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class TelemetryPoint(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Int,
    val engineOn: Boolean,
    val doorsOpen: Boolean
)

data class SimulatedVehicle(
    val id: String,
    val plate: String,
    val model: String,
    val track: List<TelemetryPoint>
) {
    /**
     * Rumo do veículo em cada ponto da sequência: a direção até o próximo ponto diferente.
     * Parado, mantém o último rumo para o ícone não "girar" durante a parada.
     */
    val headings: List<Float> by lazy {
        val result = MutableList(track.size) { 0f }
        for (i in track.indices) {
            val here = track[i]
            val next = track.getOrNull(i + 1) ?: track.first()
            result[i] = if (next.latitude != here.latitude || next.longitude != here.longitude) {
                Geo.bearing(here.latitude, here.longitude, next.latitude, next.longitude)
            } else {
                result.getOrElse(i - 1) { 0f }
            }
        }
        // Rota em loop: pontos parados no início herdam o rumo do fim da sequência.
        for (i in track.indices) {
            if (track[i].speedKmh > 0) break
            result[i] = result.last()
        }
        result
    }
}

enum class VehicleStatus { IN_TRANSIT, STOPPED }

/** Estado atual de um veículo no instante da simulação. */
data class VehicleSnapshot(
    val vehicle: SimulatedVehicle,
    /** Posição atual na sequência [SimulatedVehicle.track]. */
    val index: Int
) {
    val telemetry: TelemetryPoint get() = vehicle.track[index]

    val heading: Float get() = vehicle.headings[index]

    val status: VehicleStatus
        get() = if (telemetry.speedKmh > 0) VehicleStatus.IN_TRANSIT else VehicleStatus.STOPPED

    /** Trajeto daqui até a próxima parada (inclusive), seguindo a sequência em loop. */
    fun pathToNextStop(): List<TelemetryPoint> {
        val track = vehicle.track
        val path = mutableListOf(telemetry)
        for (step in 1 until track.size) {
            val point = track[(index + step) % track.size]
            path += point
            if (point.speedKmh == 0) break
        }
        return path
    }

    fun distanceToNextStopMeters(): Double =
        pathToNextStop().zipWithNext().sumOf { (a, b) ->
            Geo.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        }
}

/**
 * Percorre a sequência de cada veículo em loop, emitindo um novo snapshot da frota
 * a cada [tickMillis].
 */
class FleetSimulator(
    private val vehicles: List<SimulatedVehicle> = SampleFleet.vehicles,
    private val tickMillis: Long = 1_500L
) {
    fun snapshots(): Flow<List<VehicleSnapshot>> = flow {
        var tick = 0
        while (true) {
            emit(vehicles.map { VehicleSnapshot(it, tick % it.track.size) })
            delay(tickMillis)
            tick++
        }
    }
}
