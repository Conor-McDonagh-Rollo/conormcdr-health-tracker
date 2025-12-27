package ie.setu.utils

import ie.setu.domain.*
import ie.setu.domain.db.Activities
import ie.setu.domain.db.Milestones
import ie.setu.domain.db.Users
import org.jetbrains.exposed.sql.ResultRow

/** Maps a [ResultRow] from the `users` table to a [User] domain object. */
fun mapToUser(it: ResultRow) = User(
    id = it[Users.id],
    name = it[Users.name],
    email = it[Users.email]
)

/** Maps a [ResultRow] from the `activities` table to an [Activity]. */
fun mapToActivity(it: ResultRow) = Activity(
    id = it[Activities.id],
    description = it[Activities.description],
    duration = it[Activities.duration],
    calories = it[Activities.calories],
    started = it[Activities.started],
    userId = it[Activities.userId],
    steps = it[Activities.steps],
    distanceKm = it[Activities.distanceKm]
)

/** Maps a [ResultRow] from the `milestones` table to a [Milestone]. */
fun mapToMilestone(it: ResultRow) = Milestone(
    id = it[Milestones.id],
    name = it[Milestones.name],
    description = it[Milestones.description],
    targetSteps = it[Milestones.targetSteps]
)
