package ie.setu.domain.db

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime

/**
 * Exposed table definition for the `activities` table, storing
 * individual journeys and their step counts.
 */
object Activities : Table("activities") {
    const val DESCRIPTION_MAX_LENGTH = 100
    val id = integer("id").autoIncrement()
    val description = varchar("description", DESCRIPTION_MAX_LENGTH)
    val duration = double("duration")
    val calories = integer("calories")
    val steps = integer("steps").default(0)
    val distanceKm = double("distance_km").default(0.0)
    val started = datetime("started")
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(Activities.id, name = "PK_Activities_ID")
}
