package ie.setu.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Minimal OpenStreetMap reverse geocoding client using Nominatim.
 */
object OpenStreetMapService {

    private val logger = KotlinLogging.logger {}
    private val httpClient = HttpClient.newHttpClient()
    private val mapper = jsonObjectMapper()
    private val userAgent = System.getenv("OPENSTREETMAP_USER_AGENT")
        ?: "health-tracker-rest/1.0 (contact: example@example.com)"

    fun reverseGeocode(lat: Double, lon: Double): String? {
        val url = "https://nominatim.openstreetmap.org/reverse" +
            "?format=jsonv2&lat=$lat&lon=$lon&zoom=18&addressdetails=0"
        val request = HttpRequest.newBuilder(URI(url))
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .GET()
            .build()

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                logger.warn { "OSM reverse geocode failed with ${response.statusCode()} for $lat,$lon" }
                null
            } else {
                val node = mapper.readTree(response.body())
                node.path("display_name").asText(null)
            }
        } catch (e: Exception) {
            logger.warn(e) { "OSM reverse geocode request failed for $lat,$lon" }
            null
        }
    }
}
