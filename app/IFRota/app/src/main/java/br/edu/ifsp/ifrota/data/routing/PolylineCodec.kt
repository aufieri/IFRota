package br.edu.ifsp.ifrota.data.routing

/**
 * Decodifica o formato Google Encoded Polyline em lista de pares (lat, lon).
 *
 * O formato existe em duas precisões: 5 casas decimais (padrão do Google) e 6 casas
 * (`polyline6`, o que o OSRM devolve com `geometries=polyline6`). A string não diz qual é;
 * [decode] descobre sozinho.
 */
object PolylineCodec {

    /**
     * Tenta a precisão 5 e, se surgir coordenada impossível (|lat| > 90 ou |lon| > 180),
     * usa a 6. Uma polilinha de precisão 6 lida como 5 sai 10× maior e cai fora desses
     * limites em qualquer ponto a mais de ~9° de latitude ou ~18° de longitude do equador/
     * meridiano de Greenwich — caso de todo o Brasil fora do extremo norte.
     */
    fun decode(encoded: String): List<Pair<Double, Double>> {
        val p5 = decode(encoded, precision = 5)
        return if (p5.all(::isValid)) p5 else decode(encoded, precision = 6)
    }

    fun decode(encoded: String, precision: Int): List<Pair<Double, Double>> {
        if (encoded.isEmpty()) return emptyList()
        val factor = Math.pow(10.0, precision.toDouble())
        val result = mutableListOf<Pair<Double, Double>>()
        var index = 0
        var lat = 0L
        var lng = 0L
        try {
            while (index < encoded.length) {
                lat += nextValue(encoded, index).also { index = it.second }.first
                lng += nextValue(encoded, index).also { index = it.second }.first
                result.add(lat / factor to lng / factor)
            }
        } catch (_: IndexOutOfBoundsException) {
            // String truncada: devolve os pontos completos lidos até aqui.
        }
        return result
    }

    /** Lê um valor com sinal a partir de [start]; devolve (valor, próximo índice). */
    private fun nextValue(encoded: String, start: Int): Pair<Long, Int> {
        var index = start
        var shift = 0
        var partial = 0L
        var b: Int
        do {
            b = encoded[index++].code - 63
            partial = partial or ((b and 0x1f).toLong() shl shift)
            shift += 5
        } while (b >= 0x20)
        val value = if (partial and 1L != 0L) (partial shr 1).inv() else partial shr 1
        return value to index
    }

    private fun isValid(point: Pair<Double, Double>): Boolean =
        point.first in -90.0..90.0 && point.second in -180.0..180.0
}
