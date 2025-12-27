package ie.setu.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GeoUtilsTest {

    @Test
    fun `distance between identical points is zero`() {
        val distance = haversineDistanceKm(53.3498, -6.2603, 53.3498, -6.2603)
        assertEquals(0.0, distance, 0.0001)
    }

    @Test
    fun `distance between equator longitudes is about 111 km`() {
        val distance = haversineDistanceKm(0.0, 0.0, 0.0, 1.0)
        assertEquals(111.19, distance, 0.5)
    }

    @Test
    fun `step, duration, and calorie estimations are consistent`() {
        val steps = estimateStepsFromDistanceKm(1.0)
        assertEquals(1312, steps)
        assertEquals(13.12, estimateDurationMinutes(steps), 0.001)
        assertEquals(52, estimateCalories(steps))
    }
}
