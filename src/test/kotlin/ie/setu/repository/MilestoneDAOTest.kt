package ie.setu.repository

import ie.setu.domain.Milestone
import ie.setu.domain.db.Milestones
import ie.setu.domain.repository.MilestoneDAO
import ie.setu.helpers.TestDatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

/**
 * Repository-level tests for [MilestoneDAO].
 */
class MilestoneDAOTest {

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

    @Nested
    inner class CreateMilestones {

        @Test
        fun `multiple milestones added to table can be retrieved successfully`() {
            transaction {
                SchemaUtils.create(Milestones)
                val dao = MilestoneDAO()

                val m1 = Milestone(0, "Shire", "Starting point", 0)
                val m2 = Milestone(0, "Rivendell", "Elven refuge", 300_000)
                val m3 = Milestone(0, "Moria", "Dark halls", 700_000)

                val id1 = dao.save(m1)!!
                val id2 = dao.save(m2)!!
                val id3 = dao.save(m3)!!

                val all = dao.getAll()
                assertEquals(3, all.size)
                assertEquals("Shire", dao.findById(id1)?.name)
                assertEquals(300_000, dao.findById(id2)?.targetSteps)
                assertEquals("Moria", dao.findById(id3)?.name)
            }
        }
    }

    @Nested
    inner class ReadMilestones {

        @Test
        fun `get milestone by id that doesn't exist, results in null`() {
            transaction {
                SchemaUtils.create(Milestones)
                val dao = MilestoneDAO()

                assertNull(dao.findById(999))
            }
        }

        @Test
        fun `get milestone by name that exists, returns correct milestone`() {
            transaction {
                SchemaUtils.create(Milestones)
                val dao = MilestoneDAO()
                dao.save(Milestone(0, "Rivendell", "Elven refuge", 300_000))

                val found = dao.findByName("Rivendell")
                assertEquals(300_000, found?.targetSteps)
            }
        }
    }

    @Nested
    inner class UpdateMilestones {

        @Test
        fun `updating existing milestone in table results in successful update`() {
            transaction {
                SchemaUtils.create(Milestones)
                val dao = MilestoneDAO()
                val id = dao.save(Milestone(0, "Rivendell", "Elven refuge", 300_000))!!

                val updated = Milestone(id, "Rivendell", "Rest before the Misty Mountains", 320_000)
                dao.update(id, updated)

                val reloaded = dao.findById(id)
                assertEquals("Rest before the Misty Mountains", reloaded?.description)
                assertEquals(320_000, reloaded?.targetSteps)
            }
        }

        @Test
        fun `updating non-existent milestone results in no changes`() {
            transaction {
                SchemaUtils.create(Milestones)
                val dao = MilestoneDAO()

                val updated = Milestone(999, "Nowhere", "Should not exist", 1)
                val rows = dao.update(999, updated)
                assertEquals(0, rows)
                assertEquals(0, dao.getAll().size)
            }
        }
    }

    @Nested
    inner class DeleteMilestones {

        @Test
        fun `deleting a non-existent milestone results in no deletion`() {
            transaction {
                SchemaUtils.create(Milestones)
                val dao = MilestoneDAO()

                assertEquals(0, dao.getAll().size)
                val rows = dao.delete(999)
                assertEquals(0, rows)
                assertEquals(0, dao.getAll().size)
            }
        }

        @Test
        fun `deleting an existing milestone results in record being deleted`() {
            transaction {
                SchemaUtils.create(Milestones)
                val dao = MilestoneDAO()
                val id = dao.save(Milestone(0, "Rivendell", "Elven refuge", 300_000))!!

                assertEquals(1, dao.getAll().size)
                val rows = dao.delete(id)
                assertEquals(1, rows)
                assertEquals(0, dao.getAll().size)
            }
        }
    }
}

