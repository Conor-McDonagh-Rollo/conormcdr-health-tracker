package ie.setu.utils

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.InetSocketAddress
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenStreetMapServiceTest {

    private lateinit var server: HttpServer

    @BeforeAll
    fun startServer() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/reverse") { exchange ->
            val query = exchange.requestURI.query ?: ""
            val lat = Regex("lat=([^&]+)").find(query)?.groupValues?.get(1)
            val isFailure = lat == "0.0"
            val isMissingName = lat == "1.0"
            val responseBody = if (isFailure) {
                """{"error":"bad request"}"""
            } else if (isMissingName) {
                """{}"""
            } else {
                """{"display_name":"Bag End, Hobbiton"}"""
            }
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(if (isFailure) 500 else 200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()

        val baseUrl = "http://localhost:${server.address.port}"
        System.setProperty("OPENSTREETMAP_BASE_URL", baseUrl)
    }

    @AfterAll
    fun stopServer() {
        server.stop(0)
        System.clearProperty("OPENSTREETMAP_BASE_URL")
    }

    @Test
    fun `reverse geocode returns display name on success`() {
        val name = OpenStreetMapService.reverseGeocode(53.3498, -6.2603)
        assertEquals("Bag End, Hobbiton", name)
    }

    @Test
    fun `reverse geocode returns cached value on repeat call`() {
        val first = OpenStreetMapService.reverseGeocode(53.3498, -6.2603)
        val second = OpenStreetMapService.reverseGeocode(53.3498, -6.2603)
        assertEquals(first, second)
    }

    @Test
    fun `reverse geocode returns null on non-200 response`() {
        val name = OpenStreetMapService.reverseGeocode(0.0, 0.0)
        assertNull(name)
    }

    @Test
    fun `reverse geocode returns null on request failure`() {
        val previous = System.getProperty("OPENSTREETMAP_BASE_URL")
        System.setProperty("OPENSTREETMAP_BASE_URL", "http://localhost:1")
        try {
            val name = OpenStreetMapService.reverseGeocode(10.0, 10.0)
            assertNull(name)
        } finally {
            if (previous == null) {
                System.clearProperty("OPENSTREETMAP_BASE_URL")
            } else {
                System.setProperty("OPENSTREETMAP_BASE_URL", previous)
            }
        }
    }

    @Test
    fun `reverse geocode returns null when display name missing`() {
        val name = OpenStreetMapService.reverseGeocode(1.0, 0.0)
        assertNull(name)
    }

    @Test
    fun `base url trims trailing slash and defaults`() {
        val method = OpenStreetMapService::class.java.getDeclaredMethod("baseUrl")
        method.isAccessible = true
        val previous = System.getProperty("OPENSTREETMAP_BASE_URL")
        val previousOverrides = OpenStreetMapService.envOverrides

        try {
            System.setProperty("OPENSTREETMAP_BASE_URL", "http://example.com/")
            val trimmed = method.invoke(OpenStreetMapService) as String
            assertEquals("http://example.com", trimmed)

            System.clearProperty("OPENSTREETMAP_BASE_URL")
            OpenStreetMapService.envOverrides = mapOf("OPENSTREETMAP_BASE_URL" to "http://env.example.com/")
            val envUrl = method.invoke(OpenStreetMapService) as String
            assertEquals("http://env.example.com", envUrl)

            OpenStreetMapService.envOverrides = null
            val defaultUrl = method.invoke(OpenStreetMapService) as String
            assertEquals(true, defaultUrl.startsWith("https://"))
        } finally {
            if (previous == null) {
                System.clearProperty("OPENSTREETMAP_BASE_URL")
            } else {
                System.setProperty("OPENSTREETMAP_BASE_URL", previous)
            }
            OpenStreetMapService.envOverrides = previousOverrides
        }
    }

    @Test
    fun `request timeout honors minimum and defaults`() {
        val method = OpenStreetMapService::class.java.getDeclaredMethod("requestTimeout")
        method.isAccessible = true
        val previous = System.getProperty("OPENSTREETMAP_TIMEOUT_MS")
        val previousOverrides = OpenStreetMapService.envOverrides

        try {
            System.setProperty("OPENSTREETMAP_TIMEOUT_MS", "100")
            val minTimeout = method.invoke(OpenStreetMapService) as Duration
            assertEquals(Duration.ofMillis(500), minTimeout)

            System.setProperty("OPENSTREETMAP_TIMEOUT_MS", "2500")
            val customTimeout = method.invoke(OpenStreetMapService) as Duration
            assertEquals(Duration.ofMillis(2500), customTimeout)

            System.setProperty("OPENSTREETMAP_TIMEOUT_MS", "bad")
            val defaultTimeout = method.invoke(OpenStreetMapService) as Duration
            assertEquals(Duration.ofMillis(3000), defaultTimeout)

            System.clearProperty("OPENSTREETMAP_TIMEOUT_MS")
            OpenStreetMapService.envOverrides = mapOf("OPENSTREETMAP_TIMEOUT_MS" to "1500")
            val envTimeout = method.invoke(OpenStreetMapService) as Duration
            assertEquals(Duration.ofMillis(1500), envTimeout)
        } finally {
            if (previous == null) {
                System.clearProperty("OPENSTREETMAP_TIMEOUT_MS")
            } else {
                System.setProperty("OPENSTREETMAP_TIMEOUT_MS", previous)
            }
            OpenStreetMapService.envOverrides = previousOverrides
        }
    }

    @Test
    fun `user agent uses configured value or default`() {
        val method = OpenStreetMapService::class.java.getDeclaredMethod("userAgent")
        method.isAccessible = true
        val previous = System.getProperty("OPENSTREETMAP_USER_AGENT")
        val previousOverrides = OpenStreetMapService.envOverrides

        try {
            System.setProperty("OPENSTREETMAP_USER_AGENT", "health-tracker-test")
            val configured = method.invoke(OpenStreetMapService) as String
            assertEquals("health-tracker-test", configured)

            System.clearProperty("OPENSTREETMAP_USER_AGENT")
            OpenStreetMapService.envOverrides = mapOf("OPENSTREETMAP_USER_AGENT" to "env-agent")
            val envAgent = method.invoke(OpenStreetMapService) as String
            assertEquals("env-agent", envAgent)

            OpenStreetMapService.envOverrides = null
            val fallback = method.invoke(OpenStreetMapService) as String
            assertEquals(true, fallback.contains("health-tracker-rest"))
        } finally {
            if (previous == null) {
                System.clearProperty("OPENSTREETMAP_USER_AGENT")
            } else {
                System.setProperty("OPENSTREETMAP_USER_AGENT", previous)
            }
            OpenStreetMapService.envOverrides = previousOverrides
        }
    }
}
