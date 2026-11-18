package br.edu.ifsp.ifrota.poc_admin_entregas.data.routing

import kotlin.math.roundToInt

/**
 * Codificação "encoded polyline" do Google com 6 casas decimais, o formato `polyline6` do
 * OSRM. Permite guardar um trecho de rota (centenas de pontos) como uma string curta no
 * Room e no Firestore.
 */
object PolylineCodec {
    private const val FACTOR = 1e6

    fun decode(encoded: String): List<Pair<Double, Double>> {
        val result = ArrayList<Pair<Double, Double>>()
        var index = 0
        var lat = 0
        var lon = 0
        while (index < encoded.length) {
            val (dLat, afterLat) = readValue(encoded, index)
            val (dLon, afterLon) = readValue(encoded, afterLat)
            index = afterLon
            lat += dLat
            lon += dLon
            result += (lat / FACTOR) to (lon / FACTOR)
        }
        return result
    }

    fun encode(path: List<Pair<Double, Double>>): String {
        val out = StringBuilder()
        var prevLat = 0
        var prevLon = 0
        for ((lat, lon) in path) {
            val latE6 = (lat * FACTOR).roundToInt()
            val lonE6 = (lon * FACTOR).roundToInt()
            writeValue(out, latE6 - prevLat)
            writeValue(out, lonE6 - prevLon)
            prevLat = latE6
            prevLon = lonE6
        }
        return out.toString()
    }

    private fun readValue(encoded: String, start: Int): Pair<Int, Int> {
        var index = start
        var shift = 0
        var result = 0
        while (true) {
            val b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
            if (b < 0x20) break
        }
        val value = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        return value to index
    }

    private fun writeValue(out: StringBuilder, value: Int) {
        var v = if (value < 0) (value shl 1).inv() else value shl 1
        while (v >= 0x20) {
            out.append((((v and 0x1f) or 0x20) + 63).toChar())
            v = v shr 5
        }
        out.append((v + 63).toChar())
    }
}
