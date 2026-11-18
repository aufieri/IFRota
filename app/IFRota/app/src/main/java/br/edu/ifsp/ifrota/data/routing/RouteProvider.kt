package br.edu.ifsp.ifrota.data.routing

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Calcula o trajeto viário entre dois pontos (latitude, longitude). */
interface RouteProvider {
    /** Pontos do trajeto, do primeiro (origem) ao último (destino), seguindo as ruas. */
    suspend fun route(from: Pair<Double, Double>, to: Pair<Double, Double>): List<Pair<Double, Double>>
}

class RouteUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Roteador público OSRM sobre dados do OpenStreetMap — o mesmo serviço que gerou as rotas
 * fixas da versão anterior, agora consultado na criação da entrega pelo administrador. O
 * motorista não depende dele: o trecho calculado fica gravado na própria entrega.
 */
class OsrmRouteProvider(
    private val baseUrl: String = "https://router.project-osrm.org"
) : RouteProvider {

    override suspend fun route(
        from: Pair<Double, Double>,
        to: Pair<Double, Double>
    ): List<Pair<Double, Double>> = withContext(Dispatchers.IO) {
        val coordinates = "${from.second},${from.first};${to.second},${to.first}"
        val url = URL("$baseUrl/route/v1/driving/$coordinates?overview=full&geometries=polyline6")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            setRequestProperty("User-Agent", "IFRota-academic-project/1.0")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw RouteUnavailableException("Serviço de rotas indisponível (HTTP ${connection.responseCode}).")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (json.optString("code") != "Ok") {
                throw RouteUnavailableException("Não foi possível traçar a rota (${json.optString("code")}).")
            }
            val geometry = json.getJSONArray("routes").getJSONObject(0).getString("geometry")
            PolylineCodec.decode(geometry).also {
                if (it.size < 2) throw RouteUnavailableException("Rota retornada sem trajeto.")
            }
        } catch (e: RouteUnavailableException) {
            throw e
        } catch (e: Exception) {
            throw RouteUnavailableException("Sem conexão com o serviço de rotas.", e)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000
    }
}
