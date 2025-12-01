package ie.setu.domain

import org.joda.time.DateTime

/**
 * Domain model representing a single physical activity or
 * “journey” towards Mordor.
 *
 * The optional [steps] field tracks the number of steps logged
 * for this activity, used when calculating progress.
 */
data class Activity(
    var id: Int,
    var description: String,
    var duration: Double,
    var calories: Int,
    var started: DateTime,
    var userId: Int,
    var steps: Int = 0
)
