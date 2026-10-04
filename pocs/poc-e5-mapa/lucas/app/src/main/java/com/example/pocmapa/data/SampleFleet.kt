package com.example.pocmapa.data

/**
 * Frota fictícia em Araraquara-SP. Os veículos percorrem trajetos que seguem as ruas
 * ([RoadRoutes]), com paradas para simular entregas.
 */
object SampleFleet {

    /** Centro de distribuição fictício, usado também como centro inicial do mapa. */
    val DISTRIBUTION_CENTER = -21.7980 to -48.1830

    /** Quantos segundos de condução cada tick da simulação representa (acelera a animação). */
    private const val SIM_SECONDS_PER_TICK = 4.0

    val vehicles = listOf(
        SimulatedVehicle(
            id = "v1",
            plate = "ABC1D23",
            model = "Fiat Fiorino",
            track = track {
                drive(RoadRoutes.fiorinoOut, speedKmh = 45)
                stop(RoadRoutes.fiorinoOut.last(), ticks = 5, doorsOpen = true)
                drive(RoadRoutes.fiorinoBack, speedKmh = 45)
                stop(RoadRoutes.fiorinoBack.last(), ticks = 4, engineOn = false)
            }
        ),
        SimulatedVehicle(
            id = "v2",
            plate = "DEF4G56",
            model = "Mercedes Sprinter",
            track = track {
                drive(RoadRoutes.sprinterLeg1, speedKmh = 35)
                stop(RoadRoutes.sprinterLeg1.last(), ticks = 6, doorsOpen = true)
                drive(RoadRoutes.sprinterLeg2, speedKmh = 30)
                stop(RoadRoutes.sprinterLeg2.last(), ticks = 3)
                drive(RoadRoutes.sprinterLeg3, speedKmh = 38)
            }
        ),
        SimulatedVehicle(
            id = "v3",
            plate = "GHI7J89",
            model = "VW Delivery Accelo",
            track = track {
                // Estacionado durante toda a simulação.
                stop(RoadRoutes.acceloParked, ticks = 1, engineOn = false)
            }
        )
    )

    private fun track(block: TrackBuilder.() -> Unit): List<TelemetryPoint> =
        TrackBuilder().apply(block).points

    private class TrackBuilder {
        val points = mutableListOf<TelemetryPoint>()

        /** Percorre [path] avançando, a cada tick, a distância coberta na velocidade dada. */
        fun drive(path: List<Pair<Double, Double>>, speedKmh: Int) {
            val stepMeters = speedKmh / 3.6 * SIM_SECONDS_PER_TICK
            var i = 0
            for ((lat, lon) in resample(path, stepMeters)) {
                points += TelemetryPoint(
                    latitude = lat,
                    longitude = lon,
                    // Pequena variação para a velocidade não ficar constante.
                    speedKmh = speedKmh + (i++ % 3 - 1) * 4,
                    engineOn = true,
                    doorsOpen = false
                )
            }
        }

        fun stop(at: Pair<Double, Double>, ticks: Int, engineOn: Boolean = true, doorsOpen: Boolean = false) {
            repeat(ticks) {
                points += TelemetryPoint(at.first, at.second, 0, engineOn, doorsOpen)
            }
        }
    }


    private fun resample(path: List<Pair<Double, Double>>, stepMeters: Double): List<Pair<Double, Double>> {
        val result = mutableListOf(path.first())
        var carry = 0.0
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
