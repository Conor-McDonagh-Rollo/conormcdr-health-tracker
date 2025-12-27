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
}
