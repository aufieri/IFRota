package br.edu.ifsp.ifrota.data.routing

/** Decodifica o formato Google Encoded Polyline em lista de pares (lat, lon). */
object PolylineCodec {

    fun decode(encoded: String): List<Pair<Double, Double>> {
        if (encoded.isEmpty()) return emptyList()
        val result = mutableListOf<Pair<Double, Double>>()
        var index = 0
        var lat = 0
        var lng = 0
        while (index < encoded.length) {
            var shift = 0
            var partial = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                partial = partial or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lat += if (partial and 1 != 0) (partial shr 1).inv() else partial shr 1

            shift = 0
            partial = 0
            do {
                b = encoded[index++].code - 63
                partial = partial or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lng += if (partial and 1 != 0) (partial shr 1).inv() else partial shr 1

            result.add(lat / 1e5 to lng / 1e5)
        }
        return result
    }
}
