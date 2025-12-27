package ie.setu.utils

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.InetSocketAddress

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
            val responseBody = if (isFailure) {
                """{"error":"bad request"}"""
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
    fun `reverse geocode returns null on non-200 response`() {
        val name = OpenStreetMapService.reverseGeocode(0.0, 0.0)
        assertNull(name)
    }
}
