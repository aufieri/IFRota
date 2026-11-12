package com.example.pocgeocoding.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class ReverseGeocodingResult(
    val displayName: String,
    val road: String?,
    val houseNumber: String?,
    val neighborhood: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val postcode: String?
)

object GeocodingRepository {

    private const val BASE_URL = "https://nominatim.openstreetmap.org/reverse"
    private const val USER_AGENT = "IFRota-PocGeocoding/1.0"

    suspend fun reverseGeocode(lat: Double, lon: Double): Result<ReverseGeocodingResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "$BASE_URL?lat=$lat&lon=$lon&format=json&addressdetails=1"
                val connection = URL(url).openConnection().apply {
                    setRequestProperty("User-Agent", USER_AGENT)
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                val json = JSONObject(connection.getInputStream().bufferedReader().readText())
                val addr = json.optJSONObject("address")
                ReverseGeocodingResult(
                    displayName = json.optString("display_name"),
                    road = addr?.optString("road")?.ifBlank { null },
                    houseNumber = addr?.optString("house_number")?.ifBlank { null },
                    neighborhood = addr?.optString("suburb")
                        ?: addr?.optString("neighbourhood")?.ifBlank { null },
                    city = addr?.optString("city")
                        ?: addr?.optString("town")
                        ?: addr?.optString("village"),
                    state = addr?.optString("state")?.ifBlank { null },
                    country = addr?.optString("country")?.ifBlank { null },
                    postcode = addr?.optString("postcode")?.ifBlank { null }
                )
            }
        }
}
