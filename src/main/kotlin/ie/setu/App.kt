package ie.setu

import ie.setu.config.DbConfig
import ie.setu.config.JavalinConfig

/**
 * Application entry point.
 *
 * Connects to the database and starts the Javalin HTTP server
 * which exposes the REST API and Vue-powered UI.
 */
fun main() {

    DbConfig().getDbConnection()
    JavalinConfig().startJavalinService()

}
