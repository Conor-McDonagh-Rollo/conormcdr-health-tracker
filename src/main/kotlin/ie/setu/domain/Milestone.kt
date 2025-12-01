package ie.setu.domain

/**
 * Domain model for a narrative milestone on the journey.
 *
 * [targetSteps] indicates after how many cumulative steps the
 * fellowship is considered to have reached this milestone.
 */
data class Milestone(
    var id: Int,
    var name: String,
    var description: String,
    var targetSteps: Int = 0
)
