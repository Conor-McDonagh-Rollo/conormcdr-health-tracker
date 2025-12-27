package ie.setu.repository

import ie.setu.domain.Achievement
import ie.setu.domain.db.Achievements
import ie.setu.domain.repository.AchievementDAO
import ie.setu.helpers.TestDatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AchievementDAOTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupInMemoryDatabase() {
            TestDatabaseConfig.connect()
        }
    }

    @BeforeEach
    fun resetDatabase() {
        TestDatabaseConfig.reset()
    }

    private val achievement1 = Achievement(
        id = 0,
        name = "Trailblazer",
        description = "Completed 1 km",
        targetDistanceKm = 1.0,
        badgePath = "/uploads/badges/trailblazer.png"
    )
    private val achievement2 = Achievement(
        id = 0,
        name = "Pathfinder",
        description = "Completed 5 km",
        targetDistanceKm = 5.0,
        badgePath = "/uploads/badges/pathfinder.png"
    )
    private val achievement3 = Achievement(
        id = 0,
        name = "Long Haul",
        description = "Completed 20 km",
        targetDistanceKm = 20.0,
        badgePath = "/uploads/badges/long-haul.png"
    )

    @Nested
    inner class CreateAchievements {

        @Test
        fun `saving achievements allows retrieval by id`() {
            transaction {
                SchemaUtils.create(Achievements)
                val dao = AchievementDAO()
                val id = dao.save(achievement1)
                val retrieved = dao.findById(id)
                assertEquals("Trailblazer", retrieved?.name)
            }
        }
    }

    @Nested
    inner class ReadAchievements {

        @Test
        fun `getAll returns achievements ordered by distance`() {
            transaction {
                SchemaUtils.create(Achievements)
                val dao = AchievementDAO()
                dao.save(achievement2)
                dao.save(achievement1)
                dao.save(achievement3)

                val all = dao.getAll()
                assertEquals(3, all.size)
                assertEquals("Trailblazer", all[0].name)
                assertEquals("Pathfinder", all[1].name)
                assertEquals("Long Haul", all[2].name)
            }
        }

        @Test
        fun `findByTargetDistance returns achievements up to threshold`() {
            transaction {
                SchemaUtils.create(Achievements)
                val dao = AchievementDAO()
                dao.save(achievement1)
                dao.save(achievement2)
                dao.save(achievement3)

                val unlocked = dao.findByTargetDistance(5.0)
                assertEquals(2, unlocked.size)
                assertEquals("Trailblazer", unlocked[0].name)
                assertEquals("Pathfinder", unlocked[1].name)
            }
        }
    }

    @Nested
    inner class UpdateAchievements {

        @Test
        fun `updating achievement persists changes`() {
            transaction {
                SchemaUtils.create(Achievements)
                val dao = AchievementDAO()
                val id = dao.save(achievement1)
                val updated = achievement1.copy(
                    id = id,
                    description = "Completed 2 km",
                    targetDistanceKm = 2.0
                )
                dao.update(id, updated)
                val retrieved = dao.findById(id)
                assertEquals("Completed 2 km", retrieved?.description)
                assertEquals(2.0, retrieved?.targetDistanceKm ?: 0.0, 0.01)
            }
        }
    }

    @Nested
    inner class DeleteAchievements {

        @Test
        fun `deleting an achievement removes it`() {
            transaction {
                SchemaUtils.create(Achievements)
                val dao = AchievementDAO()
                val id = dao.save(achievement1)
                assertEquals(1, dao.delete(id))
                assertEquals(null, dao.findById(id))
            }
        }
    }
}
