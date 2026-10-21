package br.edu.ifsp.ifrota.data.delivery

import br.edu.ifsp.ifrota.data.simulation.DeliveryRoutes

enum class DeliveryStatus { PENDING, ACTIVE, DONE }

data class Delivery(
    val id: String,
    /** Posição na sequência do dia, a partir de 1. */
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

/**
 * Entregas atribuídas ao motorista no dia. Por enquanto vêm prontas no app; numa próxima
 * etapa serão criadas por um administrador e atribuídas a cada motorista.
 *
 * As ruas e bairros são reais (Araraquara-SP); destinatários e números são fictícios.
 */
object SampleDeliveries {

    const val DISTRIBUTION_CENTER_NAME = "IFSP Câmpus Araraquara"

    val today: List<Delivery> = listOf(
        delivery(
            order = 1,
            recipient = "Carlos Eduardo Santos",
            address = "Rua Imaculada Conceição, 342",
            neighborhood = "Quinta das Laranjeiras",
            packageType = "Caixa média",
            note = "Portão azul, tocar interfone"
        ),
        delivery(
            order = 2,
            recipient = "Fernanda Lima",
            address = "Avenida Prudente de Morais, 1500",
            neighborhood = "Vila Santana",
            packageType = "Envelope"
        ),
        delivery(
            order = 3,
            recipient = "Roberto Alves Neto",
            address = "Rua Eloy Rodrigues, 890 / Apto 74",
            neighborhood = "Vila Melhado",
            packageType = "Caixa grande",
            note = "Deixar com porteiro se não atender"
        ),
        delivery(
            order = 4,
            recipient = "Juliana Martins",
            address = "Rua Pedro Álvares Cabral, 215",
            neighborhood = "Parque do Carmo",
            packageType = "Caixa pequena",
            note = "Entregar das 14h às 18h"
        )
    )

    /** O ponto da entrega é o fim do trecho que leva até ela. */
    private fun delivery(
        order: Int,
        recipient: String,
        address: String,
        neighborhood: String,
        packageType: String,
        note: String? = null
    ): Delivery {
        val (lat, lon) = DeliveryRoutes.legs[order - 1].last()
        return Delivery(
            id = "d$order",
            order = order,
            recipient = recipient,
            address = address,
            neighborhood = neighborhood,
            city = "Araraquara - SP",
            packageType = packageType,
            note = note,
            latitude = lat,
            longitude = lon
        )
    }
}
