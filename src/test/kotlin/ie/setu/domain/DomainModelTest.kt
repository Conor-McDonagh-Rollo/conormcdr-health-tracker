package ie.setu.domain

import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DomainModelTest {

    @Test
    fun `activity data class supports copy and destructuring`() {
        val activity = Activity(
            id = 1,
            description = "Walk",
            duration = 10.0,
            calories = 50,
            started = DateTime.now(),
            userId = 2,
            steps = 100,
            distanceKm = 1.5
        )
        val copied = activity.copy(description = "Jog", steps = 200)
        val defaultCopy = activity.copy()
        val (id, description, duration, calories, started, userId, steps, distanceKm) = copied

        assertEquals(1, id)
        assertEquals("Jog", description)
        assertEquals(10.0, duration, 0.01)
        assertEquals(50, calories)
        assertEquals(activity.started.dayOfYear, started.dayOfYear)
        assertEquals(2, userId)
        assertEquals(200, steps)
        assertEquals(1.5, distanceKm, 0.01)
        assertEquals(activity, activity)
        assertEquals(activity, defaultCopy)
        assertNotEquals(activity, copied)
        assertNotEquals(activity, "not an activity")
        assertEquals(activity.hashCode(), activity.hashCode())
        assertTrue(activity.toString().contains("Activity"))

        activity.description = "Sprint"
        activity.duration = 12.5
        activity.calories = 65
        activity.started = activity.started.plusMinutes(5)
        activity.userId = 3
        activity.steps = 150
        activity.distanceKm = 2.5
        assertEquals("Sprint", activity.description)
        assertEquals(12.5, activity.duration, 0.01)
        assertEquals(65, activity.calories)
        assertEquals(3, activity.userId)
        assertEquals(150, activity.steps)
        assertEquals(2.5, activity.distanceKm, 0.01)
    }

    @Test
    fun `milestone data class supports equality and copy`() {
        val milestone = Milestone(1, "Shire", "Start", 0)
        val defaultSteps = Milestone(2, "Bree", "Crossroads")
        val same = milestone.copy()
        val changed = milestone.copy(targetSteps = 100)
        val (id, name, description, targetSteps) = milestone

        assertEquals(milestone, same)
        assertEquals(milestone, milestone)
        assertEquals(0, defaultSteps.targetSteps)
        assertNotEquals(milestone, changed)
        assertNotEquals(milestone, 123)
        assertEquals(1, id)
        assertEquals("Shire", name)
        assertEquals("Start", description)
        assertEquals(0, targetSteps)
        assertTrue(milestone.hashCode() == same.hashCode())

        milestone.name = "Bree"
        milestone.description = "Crossroads"
        milestone.targetSteps = 2500
        assertEquals("Bree", milestone.name)
        assertEquals("Crossroads", milestone.description)
        assertEquals(2500, milestone.targetSteps)
    }

    @Test
    fun `achievement data class supports copy and destructuring`() {
        val achievement = Achievement(
            id = 1,
            name = "First Kilometer",
            description = "Completed a kilometer",
            targetDistanceKm = 1.0,
            badgePath = "/uploads/badges/first.png"
        )
        val updated = achievement.copy(description = "Updated")
        val defaultCopy = achievement.copy()
        val (id, name, description, targetDistanceKm, badgePath) = updated

        assertEquals(1, id)
        assertEquals("First Kilometer", name)
        assertEquals("Updated", description)
        assertEquals(1.0, targetDistanceKm, 0.01)
        assertEquals("/uploads/badges/first.png", badgePath)
        assertEquals(achievement, achievement)
        assertEquals(achievement, defaultCopy)
        assertNotEquals(achievement, updated)
        assertNotEquals(achievement, null)
        assertEquals(achievement.hashCode(), achievement.hashCode())

        achievement.name = "Updated Name"
        achievement.description = "Updated Description"
        achievement.targetDistanceKm = 2.5
        achievement.badgePath = "/uploads/badges/updated.png"
        assertEquals("Updated Name", achievement.name)
        assertEquals("Updated Description", achievement.description)
        assertEquals(2.5, achievement.targetDistanceKm, 0.01)
        assertEquals("/uploads/badges/updated.png", achievement.badgePath)
    }

    @Test
    fun `user data class supports copy and destructuring`() {
        val user = User(1, "Frodo", "frodo@shire.me")
        val updated = user.copy(name = "Sam")
        val defaultCopy = user.copy()
        val (id, name, email) = updated

        assertEquals(1, id)
        assertEquals("Sam", name)
        assertEquals("frodo@shire.me", email)
        assertEquals(user, user)
        assertEquals(user, defaultCopy)
        assertNotEquals(user, updated)
        assertNotEquals(user, "user")
        assertEquals(user.hashCode(), user.hashCode())

        user.name = "Merry"
        user.email = "merry@shire.me"
        assertEquals("Merry", user.name)
        assertEquals("merry@shire.me", user.email)
    }
}
