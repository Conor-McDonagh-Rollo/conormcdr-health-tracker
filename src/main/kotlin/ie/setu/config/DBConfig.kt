package ie.setu.config

import ie.setu.domain.db.Activities
import ie.setu.domain.db.Milestones
import ie.setu.domain.db.Users
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.util.PSQLException

/**
 * Database configuration and bootstrap.
 *
 * In development it connects to an in-memory H2 database and
 * creates or updates the Users, Activities and Milestones tables
 * (data is cleared when the JVM stops).
 * In production it connects to a PostgreSQL instance using
 * standard `POSTGRESQL_*` environment variables.
 */
class DbConfig {

    private val logger = KotlinLogging.logger {}
    private lateinit var dbConfig: Database

    /**
     * Returns a configured [Database] connection.
     *
     * The concrete driver and URL depend on the environment
     * variables described in the class KDoc.
     */
    fun getDbConnection(): Database {

        val rawHost = System.getenv("POSTGRESQL_HOST") ?: "localhost"
        val PGHOST = rawHost.substringAfterLast("://")
        val PGPORT = System.getenv("POSTGRESQL_SERVICE_PORT") ?: "5432"
        val PGDATABASE = System.getenv("POSTGRESQL_DATABASE") ?: ""
        val PGUSER = System.getenv("POSTGRESQL_USER") ?: "sa"
        val PGPASSWORD = System.getenv("POSTGRESQL_PASSWORD") ?: ""


        try {
            logger.info { "Starting DB Connection...\nPGHOST: $PGHOST" }

            if (PGHOST == "localhost"){
                logger.info { "Using local in-memory H2 instance for development (data cleared on shutdown)." }
                dbConfig = Database.connect(
                    url = "jdbc:h2:mem:mordor-db;DB_CLOSE_DELAY=-1",
                    driver = "org.h2.Driver",
                    user = PGUSER,
                    password = PGPASSWORD
                )
                transaction {
                    SchemaUtils.createMissingTablesAndColumns(Users, Activities, Milestones)
                }
            } else {
                logger.info { "Using remote PostgreSQL instance." }
                // JDBC connection string format
                val dbUrl = "jdbc:postgresql://$rawHost:$PGPORT/$PGDATABASE"

                dbConfig = Database.connect(
                    url = dbUrl,
                    driver = "org.postgresql.Driver",
                    user = PGUSER,
                    password = PGPASSWORD
                )
            }

            transaction {
                exec("SELECT 1;") // Forces a real connection to test validity
            }
            logger.info { "DB Connected Successfully to $PGDATABASE at $rawHost:$PGPORT" }

        } catch (e: PSQLException) {
            logger.error(e) { "Error in DB Connection: ${e.message}" }
            logger.info { "Env vars used: host=$PGHOST, port=$PGPORT, user=$PGUSER, db=$PGDATABASE" }
        }

        return dbConfig
    }
}
