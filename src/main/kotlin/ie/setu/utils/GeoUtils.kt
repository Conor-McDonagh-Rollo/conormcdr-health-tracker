package ie.setu.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_KM = 6371.0
private const val STEPS_PER_KM = 1312.0

/**
 * Calculates the great-circle distance between two points in kilometers.
 */
fun haversineDistanceKm(
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double
): Double {
    val latDistance = Math.toRadians(endLat - startLat)
    val lngDistance = Math.toRadians(endLng - startLng)
    val a = sin(latDistance / 2) * sin(latDistance / 2) +
        cos(Math.toRadians(startLat)) * cos(Math.toRadians(endLat)) *
        sin(lngDistance / 2) * sin(lngDistance / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_KM * c
}

/**
 * Converts distance in kilometers to an estimated step count.
 */
fun estimateStepsFromDistanceKm(distanceKm: Double): Int =
    (distanceKm * STEPS_PER_KM).roundToInt()

/**
 * Derives an estimated duration (minutes) from steps.
 */
fun estimateDurationMinutes(steps: Int): Double =
    steps / 100.0

/**
 * Derives a rough calorie estimate from steps.
 */
fun estimateCalories(steps: Int): Int =
    (steps * 0.04).roundToInt()
