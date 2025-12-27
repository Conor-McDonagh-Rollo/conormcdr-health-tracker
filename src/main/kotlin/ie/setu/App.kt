package ie.setu

import ie.setu.config.DbConfig
import ie.setu.config.JavalinConfig
import io.javalin.Javalin

/**
 * Application entry point.
 *
 * Connects to the database and starts the Javalin HTTP server
 * which exposes the REST API and Vue-powered UI.
 */
private var runningApp: Javalin? = null

fun startApplication(
    dbConfig: DbConfig = DbConfig(),
    javalinConfig: JavalinConfig = JavalinConfig()
): Javalin {
    dbConfig.getDbConnection()
    val app = javalinConfig.startJavalinService()
    runningApp = app
    return app
}

fun stopApplication() {
    runningApp?.stop()
    runningApp = null
}

fun main() {
    startApplication()
}
