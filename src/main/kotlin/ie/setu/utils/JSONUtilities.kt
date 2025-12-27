package ie.setu.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kong.unirest.core.HttpResponse
import kong.unirest.core.JsonNode

/**
 * Creates the Jackson [ObjectMapper] instance used by Javalin
 * for JSON (de)serialization.
 */
fun jsonObjectMapper(): ObjectMapper =
    ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(JodaModule())
        .registerModule(KotlinModule.Builder().build())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

/**
 * Convenience function for converting a JSON string to a Kotlin
 * object of type [T]. Used in tests and controllers.
 */
fun <T : Any> jsonToObject(json: String, clazz: Class<T>): T =
    jacksonObjectMapper()
        .registerModule(JodaModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .readValue(json, clazz)

inline fun <reified T : Any> jsonToObject(json: String): T =
    jsonToObject(json, T::class.java)

/**
 * Converts a Unirest [HttpResponse] body into an object of type [T].
 */
fun <T : Any> jsonNodeToObject(jsonNode: HttpResponse<JsonNode>, clazz: Class<T>): T =
    jsonToObject(jsonNode.body.toString(), clazz)

inline fun <reified T : Any> jsonNodeToObject(jsonNode: HttpResponse<JsonNode>): T =
    jsonNodeToObject(jsonNode, T::class.java)
