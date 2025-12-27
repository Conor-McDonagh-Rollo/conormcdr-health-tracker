package ie.setu.domain.db

import org.jetbrains.exposed.sql.Table

/**
 * Exposed table definition for distance-based achievements.
 */
object Achievements : Table("achievements") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val description = varchar("description", 255)
    val targetDistanceKm = double("target_distance_km")
    val badgePath = varchar("badge_path", 255)

    override val primaryKey = PrimaryKey(id, name = "PK_Achievements_ID")
}
