package ie.setu.domain.db

import org.jetbrains.exposed.sql.Table

/**
 * Exposed table definition for narrative milestones along the
 * journey, including the step threshold at which each is reached.
 */
object Milestones : Table("milestones") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 20)
    val description = varchar("description", 100)
    val targetSteps = integer("target_steps").default(0)

    override val primaryKey = PrimaryKey(Milestones.id, name = "PK_Milestones_ID")
}
