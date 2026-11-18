package br.edu.ifsp.ifrota.poc_admin_entregas

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.ifsp.ifrota.poc_admin_entregas.data.local.AppDatabase
import br.edu.ifsp.ifrota.poc_admin_entregas.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.poc_admin_entregas.data.local.entity.DeliveryRecordStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumentado (precisa de emulador ou aparelho): exercita o Room de verdade. */
@RunWith(AndroidJUnit4::class)
class DeliveryDaoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun close() = db.close()

    private fun delivery(id: String, driver: String, order: Int, status: String = DeliveryRecordStatus.PENDING) =
        DeliveryEntity(id = id, driverId = driver, orderNumber = order, recipient = "R$id", status = status)

    @Test
    fun consultaPorMotoristaIsolaOsDadosEMantemAOrdem() = runBlocking {
        val dao = db.deliveryDao()
        dao.upsert(delivery("a2", "ana", 2))
        dao.upsert(delivery("a1", "ana", 1))
        dao.upsert(delivery("b1", "beto", 1))

        assertEquals(listOf("a1", "a2"), dao.observeByDriver("ana").first().map { it.id })
        assertEquals(listOf("b1"), dao.observeByDriver("beto").first().map { it.id })
        assertTrue(dao.observeByDriver("carla").first().isEmpty())
    }

    @Test
    fun soODonoConcluiASuaEntregaEIssoFicaPendenteDeSincronizacao() = runBlocking {
        val dao = db.deliveryDao()
        dao.upsert(delivery("a1", "ana", 1))

        assertEquals(0, dao.markDone("a1", "beto", 10L))
        assertEquals(DeliveryRecordStatus.PENDING, dao.getById("a1")!!.status)

        assertEquals(1, dao.markDone("a1", "ana", 10L))
        val done = dao.getById("a1")!!
        assertEquals(DeliveryRecordStatus.DONE, done.status)
        assertFalse(done.isSynced)
        assertEquals(listOf("a1"), dao.getUnsynced().map { it.id })

        dao.markAsSynced("a1", 10L)
        assertTrue(dao.getUnsynced().isEmpty())
        // Concluir de novo não altera nada.
        assertEquals(0, dao.markDone("a1", "ana", 20L))
    }

    @Test
    fun clearAllLimpaAsEntregasDeTodos() = runBlocking {
        val dao = db.deliveryDao()
        dao.upsert(delivery("a1", "ana", 1))
        dao.upsert(delivery("b1", "beto", 1))

        dao.clearAll()

        assertTrue(dao.getByDriver("ana").isEmpty())
        assertTrue(dao.getByDriver("beto").isEmpty())
    }

    @Test
    fun entregasSobrevivemAFecharEReabrirOBanco() = runBlocking {
        val name = "persistencia-test"
        context.deleteDatabase(name)
        var file = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        file.deliveryDao().upsert(delivery("a1", "ana", 1).copy(legPath = "abc"))
        file.close()

        file = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val restored = file.deliveryDao().getByDriver("ana")
        file.close()
        context.deleteDatabase(name)

        assertEquals(1, restored.size)
        assertEquals("abc", restored.single().legPath)
    }
}
