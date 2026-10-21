package br.edu.ifsp.ifrota.data.simulation

data class TelemetryPoint(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Int,
    val engineOn: Boolean,
    val doorsOpen: Boolean
)

enum class VehicleStatus { IN_TRANSIT, STOPPED }

/**
 * Trecho de uma entrega, da parada anterior (ou do CD) até o ponto de entrega: uma sequência
 * de posições ao longo do trajeto gravado na entrega, cada uma com seus dados de telemetria.
 */
class DeliveryTrip(val points: List<TelemetryPoint>) {

    val lastIndex: Int get() = points.lastIndex

    /** Rumo em cada ponto: a direção até o próximo ponto diferente (parado, mantém o anterior). */
    val headings: List<Float> by lazy {
        val result = MutableList(points.size) { 0f }
        for (i in points.indices) {
            val here = points[i]
            val next = points.getOrNull(i + 1)
            result[i] = if (next != null && (next.latitude != here.latitude || next.longitude != here.longitude)) {
                Geo.bearing(here.latitude, here.longitude, next.latitude, next.longitude)
            } else {
                result.getOrElse(i - 1) { 0f }
            }
        }
        result
    }

    fun remainingMeters(fromIndex: Int): Double =
        points.subList(fromIndex, points.size).zipWithNext().sumOf { (a, b) ->
            Geo.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        }

    /** Tempo de condução estimado até o destino, em segundos simulados. */
    fun remainingSeconds(fromIndex: Int): Double = (lastIndex - fromIndex) * SIM_SECONDS_PER_TICK

    companion object {
        /** Quantos segundos de condução cada tick da simulação representa (acelera a animação). */
        const val SIM_SECONDS_PER_TICK = 4.0

        /** Intervalo real entre dois ticks. */
        const val TICK_MILLIS = 1_500L

        private const val SPEED_KMH = 45

        /** Percorre [path] na velocidade de cruzeiro e para no fim com as portas abertas. */
        fun fromPath(path: List<Pair<Double, Double>>): DeliveryTrip {
            val points = resample(path, SPEED_KMH / 3.6 * SIM_SECONDS_PER_TICK)
                .mapIndexed { i, (lat, lon) ->
                    // Pequena variação para a velocidade não ficar constante.
                    TelemetryPoint(lat, lon, SPEED_KMH + (i % 3 - 1) * 4, engineOn = true, doorsOpen = false)
                }
            val (lat, lon) = path.last()
            return DeliveryTrip(points + TelemetryPoint(lat, lon, 0, engineOn = true, doorsOpen = true))
        }

        /** Pontos a cada [stepMeters] ao longo da polilinha (sem incluir o ponto final). */
        private fun resample(path: List<Pair<Double, Double>>, stepMeters: Double): List<Pair<Double, Double>> {
            val result = mutableListOf(path.first())
            var carry = 0.0 // distância já percorrida desde o último ponto emitido
            for ((a, b) in path.zipWithNext()) {
                val segment = Geo.distanceMeters(a.first, a.second, b.first, b.second)
                var d = stepMeters - carry
                while (d < segment) {
                    val f = d / segment
                    result += (a.first + (b.first - a.first) * f) to (a.second + (b.second - a.second) * f)
                    d += stepMeters
                }
                carry = segment - (d - stepMeters)
            }
            return result
        }
    }
}
