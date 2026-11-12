package br.edu.ifsp.ifrota.data.geocoding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class GeocodedAddress(
    val road: String?,
    val houseNumber: String?,
    val neighborhood: String?,
    val city: String?,
    val postcode: String?,
    val displayName: String
) {
    /** Endereço formatado no estilo "Rua X, 123 — Bairro — Cidade/SP" */
    val formatted: String
        get() = buildString {
            road?.let { append(it) }
            houseNumber?.let { append(", $it") }
            neighborhood?.let { append(" — $it") }
            city?.let { append(" — $it") }
        }.ifBlank { displayName }
}

object ReverseGeocodingRepository {

    private const val BASE_URL = "https://nominatim.openstreetmap.org/reverse"
    private const val USER_AGENT = "IFRota/1.0 (br.edu.ifsp.ifrota)"

    suspend fun lookup(lat: Double, lon: Double): Result<GeocodedAddress> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "$BASE_URL?lat=$lat&lon=$lon&format=json&addressdetails=1"
                val conn = URL(url).openConnection().apply {
                    setRequestProperty("User-Agent", USER_AGENT)
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                val json = JSONObject(conn.getInputStream().bufferedReader().readText())
                val addr = json.optJSONObject("address")
                GeocodedAddress(
                    road = addr?.optString("road")?.ifBlank { null },
                    houseNumber = addr?.optString("house_number")?.ifBlank { null },
                    neighborhood = addr?.optString("suburb")
                        ?: addr?.optString("neighbourhood")?.ifBlank { null },
                    city = addr?.optString("city")
                        ?: addr?.optString("town")
                        ?: addr?.optString("village"),
                    postcode = addr?.optString("postcode")?.ifBlank { null },
                    displayName = json.optString("display_name")
                )
            }
        }
}
