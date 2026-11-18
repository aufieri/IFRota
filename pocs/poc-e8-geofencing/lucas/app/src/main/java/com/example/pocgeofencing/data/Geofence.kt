package com.example.pocgeofencing.data

/**
 * Perímetro circular. [vehicleIds] define quem é monitorado: um veículo específico,
 * um grupo de veículos ou, com `null`, a frota inteira.
 */
data class Geofence(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val vehicleIds: Set<String>? = null
) {
    fun appliesTo(vehicleId: String): Boolean = vehicleIds == null || vehicleId in vehicleIds

    fun distanceTo(latitude: Double, longitude: Double): Double =
        Geo.distanceMeters(this.latitude, this.longitude, latitude, longitude)
}

enum class GeofenceTransition { ENTER, EXIT }

data class GeofenceEvent(
    val vehicleId: String,
    val plate: String,
    val geofence: Geofence,
    val transition: GeofenceTransition,
    val timestampMillis: Long
) {
    val message: String
        get() = when (transition) {
            GeofenceTransition.ENTER -> "$plate entrou em ${geofence.name}"
            GeofenceTransition.EXIT -> "$plate saiu de ${geofence.name}"
        }
}

/** Posição de um veículo num instante: o que o monitor precisa saber dele. */
data class VehiclePosition(
    val vehicleId: String,
    val plate: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Detecta entrada e saída de perímetros comparando cada nova posição com a anterior,
 * como faz a Geofencing API do Google Play Services, mas sobre posições simuladas
 * (a API do sistema só reage à localização real do aparelho).
 *
 * - Na primeira posição de um veículo o estado é só registrado, sem gerar evento
 *   (equivale a não pedir `INITIAL_TRIGGER_ENTER`).
 * - [hysteresisMeters] evita eventos repetidos quando o veículo anda rente à borda:
 *   para sair é preciso se afastar além de `raio + histerese`.
 */
class GeofenceMonitor(
    geofences: List<Geofence>,
    private val hysteresisMeters: Double = 15.0,
    private val clock: () -> Long = System::currentTimeMillis
) {
    var geofences: List<Geofence> = geofences
        private set

    /** Perímetros em que cada veículo está; ausente = veículo ainda não visto. */
    private val inside = HashMap<String, MutableSet<String>>()

    /** Troca os perímetros monitorados; os que deixaram de existir são esquecidos sem evento. */
    fun setGeofences(newGeofences: List<Geofence>) {
        geofences = newGeofences
        val ids = newGeofences.mapTo(HashSet()) { it.id }
        inside.values.forEach { it.retainAll(ids) }
    }

    /** Ids dos perímetros em que o veículo está agora. */
    fun insideOf(vehicleId: String): Set<String> = inside[vehicleId].orEmpty()

    fun update(positions: List<VehiclePosition>): List<GeofenceEvent> =
        positions.flatMap(::update)

    fun update(position: VehiclePosition): List<GeofenceEvent> {
        val known = inside[position.vehicleId]
        val current = known ?: HashSet()
        val events = mutableListOf<GeofenceEvent>()
        val now = clock()

        for (fence in geofences) {
            if (!fence.appliesTo(position.vehicleId)) continue
            val distance = fence.distanceTo(position.latitude, position.longitude)
            val wasInside = fence.id in current
            val isInside = if (wasInside) {
                distance <= fence.radiusMeters + hysteresisMeters
            } else {
                distance <= fence.radiusMeters
            }
            if (isInside == wasInside) continue

            if (isInside) current += fence.id else current -= fence.id
            if (known != null) {
                events += GeofenceEvent(
                    vehicleId = position.vehicleId,
                    plate = position.plate,
                    geofence = fence,
                    transition = if (isInside) GeofenceTransition.ENTER else GeofenceTransition.EXIT,
                    timestampMillis = now
                )
            }
        }
        inside[position.vehicleId] = current
        return events
    }

    /** Esquece um veículo: a próxima posição dele volta a ser tratada como a primeira. */
    fun reset(vehicleId: String) {
        inside.remove(vehicleId)
    }
}
