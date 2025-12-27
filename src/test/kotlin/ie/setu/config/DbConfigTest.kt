package ie.setu.config

import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class DbConfigTest {

    @Test
    fun `db config connects to local h2 database`() {
        val db = DbConfig().getDbConnection()
        assertNotNull(db)
        transaction(db) {
            exec("SELECT 1;")
        }
    }

    @Test
    fun `db config handles remote connection failures`() {
        val props = mapOf(
            "POSTGRESQL_HOST" to "127.0.0.1",
            "POSTGRESQL_SERVICE_PORT" to "1",
            "POSTGRESQL_DATABASE" to "missing",
            "POSTGRESQL_USER" to "user",
            "POSTGRESQL_PASSWORD" to "password"
        )
        val previous = props.mapValues { System.getProperty(it.key) }

        try {
            props.forEach { (key, value) -> System.setProperty(key, value) }
            val db = DbConfig().getDbConnection()
            assertNotNull(db)
        } finally {
            previous.forEach { (key, value) ->
                if (value == null) {
                    System.clearProperty(key)
                } else {
                    System.setProperty(key, value)
                }
            }
        }
    }

    @Test
    fun `db config can connect using remote settings with custom connector`() {
        val props = mapOf(
            "POSTGRESQL_HOST" to "remote-host",
            "POSTGRESQL_SERVICE_PORT" to "5432",
            "POSTGRESQL_DATABASE" to "healthtrackerdb",
            "POSTGRESQL_USER" to "sa",
            "POSTGRESQL_PASSWORD" to ""
        )
        val previous = props.mapValues { System.getProperty(it.key) }
        val connector = { _: String, _: String, user: String, password: String ->
            org.jetbrains.exposed.sql.Database.connect(
                url = "jdbc:h2:mem:remote-db;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = user,
                password = password
            )
        }

        try {
            props.forEach { (key, value) -> System.setProperty(key, value) }
            val db = DbConfig(connector).getDbConnection()
            assertNotNull(db)
            transaction(db) {
                exec("SELECT 1;")
            }
        } finally {
            previous.forEach { (key, value) ->
                if (value == null) {
                    System.clearProperty(key)
                } else {
                    System.setProperty(key, value)
                }
            }
        }
    }
}
