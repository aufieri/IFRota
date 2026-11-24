package br.edu.ifsp.ifrota.poc_admin_entregas.ui

import br.edu.ifsp.ifrota.poc_admin_entregas.data.admin.AdminDriver
import br.edu.ifsp.ifrota.poc_admin_entregas.data.admin.AdminGateway
import br.edu.ifsp.ifrota.poc_admin_entregas.data.admin.CreateDeliveriesResult
import br.edu.ifsp.ifrota.poc_admin_entregas.data.delivery.DemoDeliveryGenerator
import br.edu.ifsp.ifrota.poc_admin_entregas.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.poc_admin_entregas.ui.viewmodel.AdminViewModel
import br.edu.ifsp.ifrota.poc_admin_entregas.ui.viewmodel.DriversState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeGateway : AdminGateway {
        val drivers = MutableStateFlow(
            listOf(AdminDriver("m1", "Ana", "ana@ifsp.edu.br", "16 99999-0000", "123"))
        )
        var driversFailure: Throwable? = null
        val calls = mutableListOf<Triple<String, String, Int>>()
        var next: suspend () -> CreateDeliveriesResult = { CreateDeliveriesResult.Created(4) }

        override fun observeDrivers(): Flow<List<AdminDriver>> = flow {
            driversFailure?.let { throw it }
            drivers.collect { emit(it) }
        }

        override fun observeDeliveries(driverId: String): Flow<List<DeliveryEntity>> = MutableStateFlow(emptyList())

        override suspend fun createDeliveries(driverId: String, batchId: String, quantity: Int): CreateDeliveriesResult {
            calls += Triple(driverId, batchId, quantity)
            return next()
        }
    }

    @Test
    fun `lista os motoristas cadastrados`() = runTest(dispatcher) {
        val vm = AdminViewModel(FakeGateway())
        backgroundScope.launch { vm.drivers.collect { } }
        advanceUntilIdle()

        val state = vm.drivers.value as DriversState.Ready
        assertEquals(listOf("m1"), state.drivers.map { it.id })
    }

    @Test
    fun `falha ao ler os motoristas vira estado de erro`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply { driversFailure = IllegalStateException("PERMISSION_DENIED") }
        val vm = AdminViewModel(gateway)
        backgroundScope.launch { vm.drivers.collect { } }
        advanceUntilIdle()

        assertTrue(vm.drivers.value is DriversState.Error)
    }

    @Test
    fun `cliques repetidos enquanto cria geram uma unica requisicao`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val release = CompletableDeferred<Unit>()
        gateway.next = { release.await(); CreateDeliveriesResult.Created(4) }
        val vm = AdminViewModel(gateway)

        vm.startCreating("m1")
        vm.createDeliveries("m1")
        vm.createDeliveries("m1")
        vm.createDeliveries("m1")
        advanceUntilIdle()
        assertEquals(1, gateway.calls.size)
        assertTrue(vm.create.value.isCreating)

        release.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.create.value.isCreating)
        assertEquals(false, vm.create.value.message?.isError)
    }

    @Test
    fun `nova tentativa apos falha reaproveita o mesmo lote e apos sucesso usa outro`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val vm = AdminViewModel(gateway)
        vm.startCreating("m1")

        gateway.next = { CreateDeliveriesResult.Failure("Sem conexao") }
        vm.createDeliveries("m1")
        advanceUntilIdle()
        assertEquals("Sem conexao", vm.create.value.message?.text)
        assertEquals(true, vm.create.value.message?.isError)

        gateway.next = { CreateDeliveriesResult.Created(4) }
        vm.createDeliveries("m1")
        advanceUntilIdle()
        assertEquals(gateway.calls[0].second, gateway.calls[1].second)

        vm.createDeliveries("m1")
        advanceUntilIdle()
        assertNotEquals(gateway.calls[1].second, gateway.calls[2].second)
    }

    @Test
    fun `lote ja criado e comunicado sem duplicar`() = runTest(dispatcher) {
        val gateway = FakeGateway().apply { next = { CreateDeliveriesResult.AlreadyCreated(4) } }
        val vm = AdminViewModel(gateway)

        vm.createDeliveries("m1")
        advanceUntilIdle()

        assertEquals(false, vm.create.value.message?.isError)
        assertTrue(vm.create.value.message!!.text.contains("Nada foi duplicado"))
    }

    @Test
    fun `motorista inexistente e catalogo esgotado viram mensagens de erro`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val vm = AdminViewModel(gateway)

        gateway.next = { CreateDeliveriesResult.DriverNotFound }
        vm.createDeliveries("fantasma")
        advanceUntilIdle()
        assertEquals(true, vm.create.value.message?.isError)
        assertTrue(vm.create.value.message!!.text.contains("não está mais disponível"))

        gateway.next = { CreateDeliveriesResult.NoAddressesAvailable }
        vm.createDeliveries("m1")
        advanceUntilIdle()
        assertEquals(true, vm.create.value.message?.isError)
    }

    @Test
    fun `a quantidade enviada e a escolhida e fica dentro do limite`() = runTest(dispatcher) {
        val gateway = FakeGateway()
        val vm = AdminViewModel(gateway)

        vm.onQuantityChange(99)
        assertEquals(DemoDeliveryGenerator.MAX_QUANTITY, vm.create.value.quantity)
        vm.onQuantityChange(6)
        vm.createDeliveries("m1")
        advanceUntilIdle()

        assertEquals(6, gateway.calls.single().third)
    }
}
