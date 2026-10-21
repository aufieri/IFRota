package br.edu.ifsp.ifrota.data.simulation

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


object Geo {
    private const val METERS_PER_DEGREE = 111_320.0

    fun distanceMeters(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
        val dLat = (bLat - aLat) * METERS_PER_DEGREE
        val dLon = (bLon - aLon) * METERS_PER_DEGREE * cos(Math.toRadians(aLat))
        return sqrt(dLat * dLat + dLon * dLon)
    }

    fun bearing(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Float {
        val φ1 = Math.toRadians(aLat)
        val φ2 = Math.toRadians(bLat)
        val Δλ = Math.toRadians(bLon - aLon)
        val y = sin(Δλ) * cos(φ2)
        val x = cos(φ1) * sin(φ2) - sin(φ1) * cos(φ2) * cos(Δλ)
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }
}
