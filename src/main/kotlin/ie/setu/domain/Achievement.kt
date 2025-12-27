package ie.setu.domain

/**
 * Domain model for distance-based achievements earned by users.
 *
 * [targetDistanceKm] defines the minimum total distance (km)
 * required to unlock the achievement.
 */
data class Achievement(
    var id: Int,
    var name: String,
    var description: String,
    var targetDistanceKm: Double,
    var badgePath: String
)
