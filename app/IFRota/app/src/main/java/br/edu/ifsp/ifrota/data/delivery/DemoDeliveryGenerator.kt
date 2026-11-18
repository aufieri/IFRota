package br.edu.ifsp.ifrota.data.delivery

import java.util.Random
import kotlin.math.abs

/** Parada de demonstração: um endereço do catálogo mais os dados fictícios da entrega. */
data class DemoStop(
    val address: DemoAddress,
    val recipient: String,
    val fullAddress: String,
    val packageType: String,
    val note: String?
)

/**
 * Gera as paradas de uma criação de entregas a partir do [DemoAddressCatalog]. É a única
 * fonte de dados de demonstração do app. A escolha é determinística para um mesmo
 * `batchId`, então repetir a requisição não muda o resultado.
 *
 * Isto escolhe destinos, não otimiza trajeto: a ordem é a da lista e cada trecho é traçado
 * pelo roteador (OSRM) entre paradas consecutivas.
 */
object DemoDeliveryGenerator {
    const val MIN_QUANTITY = 1
    const val DEFAULT_QUANTITY = 4
    const val MAX_QUANTITY = 8

    private const val COORDINATE_TOLERANCE = 1e-5

    private val recipients = listOf(
        "Carlos Eduardo Santos", "Fernanda Lima", "Roberto Alves Neto", "Juliana Martins",
        "Marcos Vinícius Rocha", "Patrícia Gomes", "Ricardo Teixeira", "Camila Ferreira",
        "André Luiz Barbosa", "Larissa Mendes", "Eduardo Nogueira", "Beatriz Cardoso",
        "Thiago Ribeiro", "Aline Moreira", "Gustavo Henrique Dias", "Renata Souza",
        "Felipe Araújo", "Vanessa Pires", "Leandro Campos", "Mariana Duarte"
    )

    private val packageTypes = listOf("Envelope", "Caixa pequena", "Caixa média", "Caixa grande")

    private val notes = listOf(
        "Portão azul, tocar interfone",
        "Deixar com porteiro se não atender",
        "Entregar das 14h às 18h",
        "Frágil, manusear com cuidado",
        "Ligar ao chegar"
    )

    fun clampQuantity(quantity: Int): Int = quantity.coerceIn(MIN_QUANTITY, MAX_QUANTITY)

    /** Endereços do catálogo cuja coordenada coincide com a de alguma entrega informada. */
    fun addressIdsAt(coordinates: Collection<Pair<Double, Double>>): Set<String> =
        DemoAddressCatalog.all
            .filter { address ->
                coordinates.any { (lat, lon) ->
                    abs(lat - address.latitude) < COORDINATE_TOLERANCE &&
                        abs(lon - address.longitude) < COORDINATE_TOLERANCE
                }
            }
            .map { it.id }
            .toSet()

    /**
     * Escolhe até [quantity] paradas sem repetir endereço, percorrendo as regiões da cidade
     * em rodízio para espalhar os destinos. Endereços em [excludedAddressIds] não entram.
     * Devolve menos paradas que o pedido se o catálogo não tiver endereços suficientes.
     */
    fun generate(
        batchId: String,
        quantity: Int,
        excludedAddressIds: Set<String> = emptySet()
    ): List<DemoStop> {
        val random = Random(batchId.hashCode().toLong())

        val byZone = DemoAddressCatalog.all
            .filter { it.id !in excludedAddressIds }
            .groupBy { it.zone }
            .toSortedMap()
            .mapValues { (_, addresses) -> addresses.shuffled(random).toMutableList() }
        val zones = byZone.keys.toList().shuffled(random)

        val picked = mutableListOf<DemoAddress>()
        val wanted = clampQuantity(quantity)
        while (picked.size < wanted && byZone.values.any { it.isNotEmpty() }) {
            for (zone in zones) {
                if (picked.size == wanted) break
                byZone.getValue(zone).removeFirstOrNull()?.let { picked += it }
            }
        }

        val names = recipients.shuffled(random)
        return picked.mapIndexed { i, address ->
            DemoStop(
                address = address,
                recipient = names[i % names.size],
                fullAddress = "${address.street}, ${20 + random.nextInt(1980)}",
                packageType = packageTypes[random.nextInt(packageTypes.size)],
                note = notes.getOrNull(random.nextInt(notes.size + 3))
            )
        }
    }
}
