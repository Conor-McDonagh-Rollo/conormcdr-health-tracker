package ie.setu.domain

/**
 * Payload for creating an activity from two map points.
 */
data class ActivityMapRequest(
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double
)
