package br.edu.ifsp.ifrota.data.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolylineCodecTest {

    /** Exemplo da documentação do Google (precisão 5). */
    @Test
    fun decodesPrecision5() {
        val points = PolylineCodec.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, points.size)
        assertEquals(38.5, points[0].first, 1e-9)
        assertEquals(-120.2, points[0].second, 1e-9)
        assertEquals(43.252, points[2].first, 1e-9)
        assertEquals(-126.453, points[2].second, 1e-9)
    }

    /** Mesmos pontos em precisão 6 (formato polyline6 do OSRM), detectada automaticamente. */
    @Test
    fun detectsPrecision6() {
        val encoded = "_izlhA~rlgdF_{geC~ywl@_kwzCn`{nI"
        val points = PolylineCodec.decode(encoded)
        assertEquals(3, points.size)
        assertEquals(38.5, points[0].first, 1e-9)
        assertEquals(-120.2, points[0].second, 1e-9)
        assertEquals(43.252, points[2].first, 1e-9)
        assertEquals(-126.453, points[2].second, 1e-9)
    }

    /** Início real de um trajeto gravado nas entregas (polyline6, saindo do CD em Araraquara). */
    @Test
    fun decodesDeliveryLegFromDistributionCenter() {
        val first = PolylineCodec.decode("zmrph@hfp}zA").first()
        assertEquals(-21.784302, first.first, 1e-9)
        assertEquals(-48.210549, first.second, 1e-9)
    }

    @Test
    fun emptyAndTruncatedStrings() {
        assertTrue(PolylineCodec.decode("").isEmpty())
        // Corta no meio do segundo ponto: mantém só o primeiro, sem exceção.
        assertEquals(1, PolylineCodec.decode("_p~iF~ps|U_ulL").size)
    }
}
