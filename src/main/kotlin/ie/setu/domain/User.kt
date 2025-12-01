package ie.setu.domain

/**
 * Domain model representing a registered user/companion in the
 * health tracker.
 */
data class User(
    var id: Int,
    var name: String,
    var email: String
)
