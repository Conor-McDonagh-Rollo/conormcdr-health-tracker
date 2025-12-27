package ie.setu.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal OpenStreetMap reverse geocoding client using Nominatim.
 */
object OpenStreetMapService {

    private val logger = KotlinLogging.logger {}
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(requestTimeout())
        .version(HttpClient.Version.HTTP_1_1)
        .build()
    private val mapper = jsonObjectMapper()
    private val cache = ConcurrentHashMap<String, String>()

    private fun baseUrl(): String {
        val configured = System.getProperty("OPENSTREETMAP_BASE_URL")
            ?: System.getenv("OPENSTREETMAP_BASE_URL")
            ?: "https://nominatim.openstreetmap.org"
        return configured.trimEnd('/')
    }

    private fun requestTimeout(): Duration {
        val raw = System.getProperty("OPENSTREETMAP_TIMEOUT_MS")
            ?: System.getenv("OPENSTREETMAP_TIMEOUT_MS")
        val timeoutMs = raw?.toLongOrNull()?.coerceAtLeast(500) ?: 3000L
        return Duration.ofMillis(timeoutMs)
    }

    private fun userAgent(): String =
        System.getProperty("OPENSTREETMAP_USER_AGENT")
            ?: System.getenv("OPENSTREETMAP_USER_AGENT")
            ?: "health-tracker-rest/1.0 (contact: example@example.com)"

    fun reverseGeocode(lat: Double, lon: Double): String? {
        val key = "%.5f,%.5f".format(lat, lon)
        val cached = cache[key]
        if (cached != null) {
            return cached
        }

        val url = "${baseUrl()}/reverse" +
            "?format=jsonv2&lat=$lat&lon=$lon&zoom=18&addressdetails=0"
        val request = HttpRequest.newBuilder(URI(url))
            .header("User-Agent", userAgent())
            .header("Accept", "application/json")
            .timeout(requestTimeout())
            .GET()
            .build()

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                logger.warn { "OSM reverse geocode failed with ${response.statusCode()} for $lat,$lon" }
                null
            } else {
                val node = mapper.readTree(response.body())
                node.path("display_name").asText(null)?.also { cache[key] = it }
            }
        } catch (e: Exception) {
            logger.warn(e) { "OSM reverse geocode request failed for $lat,$lon" }
            null
        }
    }
}
