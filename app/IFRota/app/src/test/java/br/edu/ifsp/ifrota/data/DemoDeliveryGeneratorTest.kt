package br.edu.ifsp.ifrota.data

import br.edu.ifsp.ifrota.data.delivery.DemoAddressCatalog
import br.edu.ifsp.ifrota.data.delivery.DemoDeliveryGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDeliveryGeneratorTest {

    @Test
    fun `todo endereco do catalogo esta dentro de Araraquara`() {
        DemoAddressCatalog.all.forEach {
            assertTrue("${it.id} fora da area urbana", it.latitude in -21.85..-21.70 && it.longitude in -48.25..-48.10)
        }
    }

    @Test
    fun `catalogo nao tem ids nem coordenadas repetidos`() {
        val all = DemoAddressCatalog.all
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertEquals(all.size, all.map { it.latitude to it.longitude }.toSet().size)
    }

    @Test
    fun `gera a quantidade pedida com todos os campos preenchidos`() {
        val stops = DemoDeliveryGenerator.generate("lote-1", 5)

        assertEquals(5, stops.size)
        stops.forEach {
            assertTrue(it.recipient.isNotBlank())
            assertTrue(it.fullAddress.startsWith(it.address.street))
            assertTrue(it.packageType.isNotBlank())
        }
    }

    @Test
    fun `nao repete endereco nem destinatario no mesmo lote`() {
        val stops = DemoDeliveryGenerator.generate("lote-2", DemoDeliveryGenerator.MAX_QUANTITY)

        assertEquals(stops.size, stops.map { it.address.id }.toSet().size)
        assertEquals(stops.size, stops.map { it.recipient }.toSet().size)
    }

    @Test
    fun `espalha os destinos por regioes diferentes`() {
        val stops = DemoDeliveryGenerator.generate("lote-3", 4)

        assertEquals(4, stops.map { it.address.zone }.toSet().size)
    }

    @Test
    fun `o mesmo lote gera sempre o mesmo resultado e lotes diferentes variam`() {
        assertEquals(DemoDeliveryGenerator.generate("igual", 4), DemoDeliveryGenerator.generate("igual", 4))
        val results = listOf("a", "b", "c", "d").map { id -> DemoDeliveryGenerator.generate(id, 4).map { it.address.id } }
        assertTrue(results.toSet().size > 1)
    }

    @Test
    fun `limita a quantidade ao intervalo permitido`() {
        assertEquals(DemoDeliveryGenerator.MAX_QUANTITY, DemoDeliveryGenerator.generate("x", 99).size)
        assertEquals(DemoDeliveryGenerator.MIN_QUANTITY, DemoDeliveryGenerator.generate("x", -3).size)
    }

    @Test
    fun `respeita enderecos excluidos e devolve menos quando o catalogo acaba`() {
        val excluded = DemoAddressCatalog.all.drop(2).map { it.id }.toSet()

        val stops = DemoDeliveryGenerator.generate("lote-4", 8, excluded)

        assertEquals(2, stops.size)
        assertTrue(stops.none { it.address.id in excluded })
        assertTrue(DemoDeliveryGenerator.generate("lote-4", 3, DemoAddressCatalog.all.map { it.id }.toSet()).isEmpty())
    }

    @Test
    fun `identifica enderecos do catalogo pela coordenada`() {
        val first = DemoAddressCatalog.all.first()

        val ids = DemoDeliveryGenerator.addressIdsAt(listOf(first.latitude to first.longitude, 0.0 to 0.0))

        assertEquals(setOf(first.id), ids)
    }
}
