package br.edu.ifsp.ifrota.poc_admin_entregas.data

import br.edu.ifsp.ifrota.poc_admin_entregas.data.routing.PolylineCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolylineCodecTest {

    @Test
    fun `codifica e decodifica sem perder precisao de 6 casas`() {
        val path = listOf(
            -21.784300 to -48.210550,
            -21.784340 to -48.210600,
            -21.776370 to -48.193680,
            -21.804930 to -48.179710
        )

        val decoded = PolylineCodec.decode(PolylineCodec.encode(path))

        assertEquals(path.size, decoded.size)
        path.zip(decoded).forEach { (expected, actual) ->
            assertEquals(expected.first, actual.first, 1e-6)
            assertEquals(expected.second, actual.second, 1e-6)
        }
    }

    @Test
    fun `coordenadas positivas e negativas fazem o caminho de ida e volta`() {
        val path = listOf(38.5 to -120.2, 40.7 to -120.95, 43.252 to -126.453)

        val decoded = PolylineCodec.decode(PolylineCodec.encode(path))

        assertEquals(38.5, decoded[0].first, 1e-6)
        assertEquals(-126.453, decoded[2].second, 1e-6)
    }

    @Test
    fun `texto vazio vira lista vazia`() {
        assertTrue(PolylineCodec.decode("").isEmpty())
        assertEquals("", PolylineCodec.encode(emptyList()))
    }
}
