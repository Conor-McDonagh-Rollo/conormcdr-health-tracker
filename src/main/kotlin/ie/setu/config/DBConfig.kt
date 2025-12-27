package ie.setu.config

import ie.setu.domain.db.Activities
import ie.setu.domain.db.Achievements
import ie.setu.domain.db.Milestones
import ie.setu.domain.db.Users
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Database configuration and bootstrap.
 *
 * In development it connects to an in-memory H2 database and
 * creates or updates the Users, Activities and Milestones tables
 * (data is cleared when the JVM stops).
 * In production it connects to a PostgreSQL instance using
 * standard `POSTGRESQL_*` environment variables.
 */
class DbConfig(
    private val connector: (String, String, String, String) -> Database = { url, driver, user, password ->
        Database.connect(url, driver, user, password)
    }
) {

    private val logger = KotlinLogging.logger("DbConfig")
    private lateinit var dbConfig: Database

    /**
     * Returns a configured [Database] connection.
     *
     * The concrete driver and URL depend on the environment
     * variables described in the class KDoc.
     */
    fun getDbConnection(): Database {

        val rawHost = envOrProperty("POSTGRESQL_HOST") ?: "localhost"
        val PGHOST = rawHost.substringAfterLast("://")
        val PGPORT = envOrProperty("POSTGRESQL_SERVICE_PORT") ?: "5432"
        val PGDATABASE = envOrProperty("POSTGRESQL_DATABASE") ?: ""
        val PGUSER = envOrProperty("POSTGRESQL_USER") ?: "sa"
        val PGPASSWORD = envOrProperty("POSTGRESQL_PASSWORD") ?: ""


        try {
            logger.info { "Starting DB Connection...\nPGHOST: $PGHOST" }

            if (PGHOST == "localhost"){
                logger.info { "Using local in-memory H2 instance for development (data cleared on shutdown)." }
                dbConfig = connector(
                    "jdbc:h2:mem:mordor-db;DB_CLOSE_DELAY=-1",
                    "org.h2.Driver",
                    PGUSER,
                    PGPASSWORD
                )
                transaction {
                    SchemaUtils.createMissingTablesAndColumns(Users, Activities, Milestones, Achievements)
                }
            } else {
                logger.info { "Using remote PostgreSQL instance." }
                // JDBC connection string format
                val dbUrl = "jdbc:postgresql://$rawHost:$PGPORT/$PGDATABASE"

                dbConfig = connector(
                    dbUrl,
                    "org.postgresql.Driver",
                    PGUSER,
                    PGPASSWORD
                )
            }

            transaction {
                exec("SELECT 1;") // Forces a real connection to test validity
            }
            logger.info { "DB Connected Successfully to $PGDATABASE at $rawHost:$PGPORT" }

        } catch (e: Exception) {
            logger.error(e) { "Error in DB Connection: ${e.message}" }
            logger.info { "Env vars used: host=$PGHOST, port=$PGPORT, user=$PGUSER, db=$PGDATABASE" }
        }

        return dbConfig
    }

    private fun envOrProperty(name: String): String? {
        return System.getProperty(name) ?: System.getenv(name)
    }
}
