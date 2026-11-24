package br.edu.ifsp.ifrota.poc_admin_entregas.data.routing

import kotlin.math.cos
import kotlin.math.sqrt

/** Medidas de um trecho gravado na entrega (polyline codificada). */
object RouteMetrics {
    private const val METERS_PER_DEGREE = 111_320.0

    /** Comprimento do trecho em metros, somando os segmentos entre pontos consecutivos. */
    fun pathMeters(encodedPath: String): Double =
        PolylineCodec.decode(encodedPath).zipWithNext().sumOf { (a, b) ->
            val dLat = (b.first - a.first) * METERS_PER_DEGREE
            val dLon = (b.second - a.second) * METERS_PER_DEGREE * cos(Math.toRadians(a.first))
            sqrt(dLat * dLat + dLon * dLon)
        }

    fun formatKm(meters: Double): String =
        if (meters < 1000) "${meters.toInt()} m" else "%.1f km".format(java.util.Locale.forLanguageTag("pt-BR"), meters / 1000)
}
