package br.edu.ifsp.ifrota.data.delivery

enum class DeliveryStatus { PENDING, ACTIVE, DONE }

data class Delivery(
    val id: String,
    /** Posição na sequência do motorista, a partir de 1. */
    val order: Int,
    val recipient: String,
    val address: String,
    val neighborhood: String,
    val city: String,
    val packageType: String,
    val note: String? = null,
    val latitude: Double,
    val longitude: Double
)

/** Ponto de partida de toda rota: o IFSP Câmpus Araraquara. */
object DistributionCenter {
    const val NAME = "IFSP Câmpus Araraquara"
    const val LATITUDE = -21.78430
    const val LONGITUDE = -48.21055

    val position: Pair<Double, Double> = LATITUDE to LONGITUDE
}
