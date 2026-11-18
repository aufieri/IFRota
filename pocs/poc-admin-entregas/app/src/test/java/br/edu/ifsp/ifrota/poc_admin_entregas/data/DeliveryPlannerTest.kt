package br.edu.ifsp.ifrota.poc_admin_entregas.data

import br.edu.ifsp.ifrota.poc_admin_entregas.data.delivery.DeliveryBatchBuilder
import br.edu.ifsp.ifrota.poc_admin_entregas.data.delivery.DeliveryPlanner
import br.edu.ifsp.ifrota.poc_admin_entregas.data.delivery.DemoAddressCatalog
import br.edu.ifsp.ifrota.poc_admin_entregas.data.delivery.DemoDeliveryGenerator
import br.edu.ifsp.ifrota.poc_admin_entregas.data.delivery.DistributionCenter
import br.edu.ifsp.ifrota.poc_admin_entregas.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.poc_admin_entregas.data.local.entity.DeliveryRecordStatus
import br.edu.ifsp.ifrota.poc_admin_entregas.data.routing.PolylineCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DeliveryPlannerTest {

    private fun ready(plan: DeliveryPlanner.Plan) = plan as DeliveryPlanner.Plan.Ready

    private fun existing(order: Int, addressIndex: Int, status: String = DeliveryRecordStatus.PENDING): DeliveryEntity {
        val address = DemoAddressCatalog.all[addressIndex]
        return DeliveryEntity(
            id = "antigo_$order", driverId = "m1", orderNumber = order,
            latitude = address.latitude, longitude = address.longitude, status = status
        )
    }

    @Test
    fun `motorista sem entregas comeca no CD e na posicao 1`() {
        val plan = ready(DeliveryPlanner.plan(emptyList(), "lote", 4))

        assertEquals(4, plan.stops.size)
        assertEquals(DistributionCenter.position, plan.start)
        assertEquals(1, plan.firstOrder)
    }

    @Test
    fun `novo lote continua a fila a partir da ultima parada`() {
        val queue = listOf(existing(1, 0), existing(2, 1))

        val plan = ready(DeliveryPlanner.plan(queue, "lote", 3))

        assertEquals(3, plan.firstOrder)
        assertEquals(DemoAddressCatalog.all[1].latitude to DemoAddressCatalog.all[1].longitude, plan.start)
    }

    @Test
    fun `nao repete enderecos ainda pendentes mas libera os ja entregues`() {
        val queue = listOf(existing(1, 0, DeliveryRecordStatus.DONE), existing(2, 1))
        val pendingId = DemoAddressCatalog.all[1].id

        repeat(20) { n ->
            val plan = ready(DeliveryPlanner.plan(queue, "lote-$n", DemoDeliveryGenerator.MAX_QUANTITY))
            assertTrue(plan.stops.none { it.address.id == pendingId })
        }
    }

    @Test
    fun `lote ja gravado e reconhecido e nao gera nada novo`() {
        val batch = "lote-repetido"
        val queue = listOf(existing(1, 0).copy(id = DeliveryBatchBuilder.deliveryId(batch, 1)))

        val plan = DeliveryPlanner.plan(queue, batch, 4)

        assertEquals(DeliveryPlanner.Plan.AlreadyCreated(1), plan)
    }

    @Test
    fun `sem enderecos disponiveis nao cria nada`() {
        val queue = DemoAddressCatalog.all.indices.map { existing(it + 1, it) }

        assertEquals(DeliveryPlanner.Plan.NoAddressesAvailable, DeliveryPlanner.plan(queue, "novo", 3))
    }

    @Test
    fun `o lote montado liga cada entrega ao motorista e preserva a ordem`() {
        val stops = DemoDeliveryGenerator.generate("lote-b", 3)
        val legs = stops.map {
            listOf(it.address.latitude - 0.001 to it.address.longitude, it.address.latitude to it.address.longitude)
        }

        val deliveries = DeliveryBatchBuilder.build("motorista-9", "lote-b", stops, legs, firstOrder = 5, now = 1_000L)

        assertEquals(listOf("lote-b_1", "lote-b_2", "lote-b_3"), deliveries.map { it.id })
        assertEquals(listOf(5, 6, 7), deliveries.map { it.orderNumber })
        assertTrue(deliveries.all { it.driverId == "motorista-9" && it.status == DeliveryRecordStatus.PENDING })
        assertEquals(2, PolylineCodec.decode(deliveries[1].legPath).size)
        assertEquals(stops[2].address.latitude, deliveries[2].latitude, 0.0)
    }

    @Test
    fun `paradas e trechos em quantidades diferentes sao recusados`() {
        try {
            DeliveryBatchBuilder.build("m", "l", DemoDeliveryGenerator.generate("l", 2), emptyList(), 1, 0L)
            fail("deveria recusar")
        } catch (_: IllegalArgumentException) {
        }
    }
}
