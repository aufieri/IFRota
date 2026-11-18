package br.edu.ifsp.ifrota.poc_admin_entregas.data.delivery

/** Ponto de partida de toda rota: o IFSP Câmpus Araraquara. */
object DistributionCenter {
    const val NAME = "IFSP Câmpus Araraquara"
    const val LATITUDE = -21.78430
    const val LONGITUDE = -48.21055

    val position: Pair<Double, Double> = LATITUDE to LONGITUDE
}
