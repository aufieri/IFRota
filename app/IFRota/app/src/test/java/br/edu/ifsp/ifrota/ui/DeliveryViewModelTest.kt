package br.edu.ifsp.ifrota.ui

import br.edu.ifsp.ifrota.data.delivery.DistributionCenter
import br.edu.ifsp.ifrota.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.data.local.entity.DeliveryRecordStatus
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import br.edu.ifsp.ifrota.data.routing.PolylineCodec
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryPhase
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** A rota do motorista é montada a partir das entregas gravadas, não de dados fixos. */
@OptIn(ExperimentalCoroutinesApi::class)
class DeliveryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val vehicles = MutableStateFlow(listOf(VehicleEntity(id = "v1", plate = "ABC1D23")))
    private val deliveries = MutableStateFlow<List<DeliveryEntity>>(emptyList())
    private val confirmed = mutableListOf<String>()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = DeliveryViewModel(vehicles, deliveries) { confirmed += it }

    /** Trecho curto (cerca de 300 m) saindo de [fromLat]. */
    private fun delivery(order: Int, status: String = DeliveryRecordStatus.PENDING, legPath: String? = null): DeliveryEntity {
        val lat = -21.78 - order * 0.003
        return DeliveryEntity(
            id = "d$order", driverId = "m1", orderNumber = order, recipient = "Destinatario $order",
            address = "Rua $order, 10", neighborhood = "Bairro $order", city = "Araraquara - SP",
            latitude = lat, longitude = -48.20, status = status,
            legPath = legPath ?: PolylineCodec.encode(listOf(lat + 0.003 to -48.20, lat + 0.0015 to -48.20, lat to -48.20))
        )
    }

    @Test
    fun `motorista sem entregas fica parado no CD sem quebrar`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.loaded)
        assertFalse(state.hasDeliveries)
        assertTrue(state.allDone)
        assertEquals(null, state.currentDelivery)
        assertEquals(DistributionCenter.LATITUDE, state.position.latitude, 0.0)
        assertTrue(state.remainingPath.isEmpty())
        assertFalse(state.canStart)

        vm.start()
        advanceUntilIdle()
        assertEquals(DeliveryPhase.PLANNING, vm.state.value.phase)
    }

    @Test
    fun `entregas gravadas viram fila numerada com um trecho cada`() = runTest(dispatcher) {
        deliveries.value = listOf(delivery(2), delivery(1), delivery(3))
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(listOf("d1", "d2", "d3"), state.deliveries.map { it.id })
        assertEquals(listOf(1, 2, 3), state.deliveries.map { it.order })
        assertEquals(3, state.legs.size)
        assertEquals(0, state.current)
        assertEquals("Destinatario 1", state.currentDelivery?.recipient)
    }

    @Test
    fun `a entrega atual e a primeira ainda nao concluida`() = runTest(dispatcher) {
        deliveries.value = listOf(delivery(1, DeliveryRecordStatus.DONE), delivery(2), delivery(3))
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(1, vm.state.value.current)
        assertEquals(1, vm.state.value.doneCount)
        assertEquals("d2", vm.state.value.currentDelivery?.id)
    }

    @Test
    fun `percorre o trecho, confirma e grava a entrega como concluida`() = runTest(dispatcher) {
        deliveries.value = listOf(delivery(1), delivery(2))
        val vm = viewModel()
        advanceUntilIdle()

        vm.start()
        advanceUntilIdle()
        assertEquals(DeliveryPhase.ARRIVED, vm.state.value.phase)

        vm.confirmDelivery()
        advanceUntilIdle()

        assertEquals(listOf("d1"), confirmed)
        assertEquals(1, vm.state.value.current)
        assertEquals(DeliveryPhase.PLANNING, vm.state.value.phase)
    }

    @Test
    fun `confirmar antes de chegar nao conclui nada`() = runTest(dispatcher) {
        deliveries.value = listOf(delivery(1))
        val vm = viewModel()
        advanceUntilIdle()

        vm.confirmDelivery()
        advanceUntilIdle()

        assertTrue(confirmed.isEmpty())
        assertEquals(0, vm.state.value.current)
    }

    @Test
    fun `entrega nova atribuida depois de tudo concluido reabre a rota`() = runTest(dispatcher) {
        deliveries.value = listOf(delivery(1, DeliveryRecordStatus.DONE))
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value.allDone)

        deliveries.value = deliveries.value + delivery(2)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.allDone)
        assertEquals("d2", state.currentDelivery?.id)
        assertEquals(2, state.legs.size)
    }

    @Test
    fun `entrega sem trajeto gravado nao derruba a tela`() = runTest(dispatcher) {
        deliveries.value = listOf(delivery(1, legPath = ""))
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(1, state.legs.size)
        assertEquals(state.deliveries.first().latitude, state.position.latitude, 1e-9)
    }

    @Test
    fun `sem veiculo nao ha como iniciar`() = runTest(dispatcher) {
        vehicles.value = emptyList()
        deliveries.value = listOf(delivery(1))
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.state.value.canStart)
    }
}
