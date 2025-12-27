package ie.setu.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ActivityMapRequestTest {

    @Test
    fun `activity map request holds coordinates`() {
        val request = ActivityMapRequest(
            startLat = 53.3498,
            startLng = -6.2603,
            endLat = 53.3438,
            endLng = -6.2546
        )

        assertEquals(53.3498, request.startLat)
        assertEquals(-6.2603, request.startLng)
        assertEquals(53.3438, request.endLat)
        assertEquals(-6.2546, request.endLng)
        assertEquals(request, request.copy())
    }
}
