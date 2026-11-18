package br.edu.ifsp.ifrota.data.delivery

import br.edu.ifsp.ifrota.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.data.local.entity.DeliveryRecordStatus
import br.edu.ifsp.ifrota.data.routing.PolylineCodec

/** Monta as entidades de um lote de entregas a partir das paradas e dos trechos já traçados. */
object DeliveryBatchBuilder {

    /** O id do documento é `<lote>_<n>`: refazer o mesmo lote só pode escrever os mesmos documentos. */
    fun deliveryId(batchId: String, position: Int) = "${batchId}_$position"

    fun belongsToBatch(deliveryId: String, batchId: String) = deliveryId.startsWith("${batchId}_")

    /**
     * @param legs trecho i vai da parada anterior (ou do CD) até a parada i.
     * @param firstOrder posição na fila do motorista da primeira entrega do lote.
     */
    fun build(
        driverId: String,
        batchId: String,
        stops: List<DemoStop>,
        legs: List<List<Pair<Double, Double>>>,
        firstOrder: Int,
        now: Long
    ): List<DeliveryEntity> {
        require(stops.size == legs.size) { "Cada parada precisa de um trecho de rota." }
        return stops.mapIndexed { i, stop ->
            DeliveryEntity(
                id = deliveryId(batchId, i + 1),
                driverId = driverId,
                orderNumber = firstOrder + i,
                recipient = stop.recipient,
                address = stop.fullAddress,
                neighborhood = stop.address.neighborhood,
                city = DemoAddressCatalog.CITY,
                packageType = stop.packageType,
                note = stop.note,
                latitude = stop.address.latitude,
                longitude = stop.address.longitude,
                status = DeliveryRecordStatus.PENDING,
                legPath = PolylineCodec.encode(legs[i]),
                createdAt = now,
                isSynced = true,
                updatedAt = now
            )
        }
    }
}
