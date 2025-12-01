package ie.setu.helpers

import ie.setu.domain.db.Users
import ie.setu.domain.db.Activities
import ie.setu.domain.db.Milestones
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Shared configuration for tests that use an in-memory H2 database.
 *
 * Creates and resets the Users, Activities and Milestones tables
 * between test cases.
 */
object TestDatabaseConfig {

    private var initialised = false

    fun connect() {
        if (!initialised) {
            Database.connect(
                url = "jdbc:h2:mem:test-db;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )
            initialised = true

            transaction {
                SchemaUtils.create(Users, Activities, Milestones)
            }
        }
    }

    fun reset() {
        transaction {
            SchemaUtils.drop(Users, Activities, Milestones)
            SchemaUtils.create(Users, Activities, Milestones)
        }
    }
}
