package ie.setu.controllers

import ie.setu.domain.Activity
import ie.setu.domain.Achievement
import ie.setu.domain.Milestone
import ie.setu.domain.User
import ie.setu.domain.db.Activities
import ie.setu.domain.db.Users
import ie.setu.helpers.ServerContainer
import ie.setu.helpers.TestDatabaseConfig
import ie.setu.helpers.activities
import ie.setu.helpers.nonExistingEmail
import ie.setu.helpers.populateUserTable
import ie.setu.helpers.updatedCalories
import ie.setu.helpers.updatedDescription
import ie.setu.helpers.updatedDuration
import ie.setu.helpers.updatedStarted
import ie.setu.helpers.users
import ie.setu.helpers.validEmail
import ie.setu.helpers.validName
import ie.setu.utils.jsonNodeToObject
import ie.setu.utils.jsonToObject
import kong.unirest.core.HttpResponse
import kong.unirest.core.JsonNode
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import kong.unirest.core.Unirest
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.assertNotEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HealthTrackerTest {

    private val app = ServerContainer.instance
    private val origin = "http://localhost:" + app.port()
    private lateinit var osmServer: HttpServer

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupInMemoryDatabase() {
            TestDatabaseConfig.connect()
        }
    }

    @BeforeAll
    fun startOsmStub() {
        osmServer = HttpServer.create(InetSocketAddress(0), 0)
        osmServer.createContext("/reverse") { exchange ->
            val query = exchange.requestURI.query ?: ""
            val responseBody = if (query.contains("lat=0.0")) {
                """{"error":"bad request"}"""
            } else {
                """{"display_name":"Bag End, Hobbiton"}"""
            }
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(if (query.contains("lat=0.0")) 500 else 200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        osmServer.start()
        System.setProperty("OPENSTREETMAP_BASE_URL", "http://localhost:${osmServer.address.port}")
    }

    @AfterAll
    fun stopOsmStub() {
        osmServer.stop(0)
        System.clearProperty("OPENSTREETMAP_BASE_URL")
    }

    @BeforeEach
    fun resetDatabase() {
        TestDatabaseConfig.reset()
    }

    @Nested
    inner class CreateUsers {
        @Test
        fun `add a user with correct details returns a 201 response`() {

            //Arrange & Act & Assert
            //    add the user and verify return code (using fixture data)
            val addResponse = addUser(validName, validEmail)
            assertEquals(201, addResponse.status)

            //Assert - retrieve the added user from the database and verify return code
            val retrieveResponse = retrieveUserByEmail(validEmail)
            assertEquals(200, retrieveResponse.status)

            //Assert - verify the contents of the retrieved user
            val retrievedUser: User = jsonToObject(addResponse.body.toString())
            assertEquals(validEmail, retrievedUser.email)
            assertEquals(validName, retrievedUser.name)

            //After - restore the db to previous state by deleting the added user
            val deleteResponse = deleteUser(retrievedUser.id)
            assertEquals(204, deleteResponse.status)
        }

        @Test
        fun `adding a user without admin role returns 403 response`() {
            val addResponse = addUser(validName, validEmail, role = "user")
            assertEquals(403, addResponse.status)
        }

        @Test
        fun `multiple users added to table can be retrieved successfully`() {
            transaction {
                val userDAO = populateUserTable()

                assertEquals(3, userDAO.getAll().size)
                assertEquals(users[0].name, userDAO.findByEmail(users[0].email)?.name)
                assertEquals(users[1].name, userDAO.findByEmail(users[1].email)?.name)
                assertEquals(users[2].name, userDAO.findByEmail(users[2].email)?.name)
            }
        }
    }

    @Nested
    inner class ReadUsers {

        @Test
        fun `get all users returns 404 when none exist`() {
            val response = Unirest.get(origin + "/api/users/").asString()
            assertEquals(404, response.status)
        }

        @Test
        fun `get all users returns 200 when users exist`() {
            addUser(validName, validEmail)

            val response = Unirest.get(origin + "/api/users/").asString()
            assertEquals(200, response.status)
            val retrievedUsers: ArrayList<User> = jsonToObject(response.body.toString())
            assertNotEquals(0, retrievedUsers.size)
        }


        @Test
        fun `get user by id when user does not exist returns 404 response`() {

            //Arrange - test data for user id
            val id = Integer.MIN_VALUE

            // Act - attempt to retrieve the non-existent user from the database
            val retrieveResponse = Unirest.get(origin + "/api/users/${id}").asString()

            // Assert -  verify return code
            assertEquals(404, retrieveResponse.status)
        }

        @Test
        fun `get user by email when user does not exist returns 404 response`() {
            // Arrange & Act - attempt to retrieve the non-existent user from the database
            val retrieveResponse = Unirest.get(origin + "/api/users/email/${nonExistingEmail}").asString()
            // Assert -  verify return code
            assertEquals(404, retrieveResponse.status)
        }

        @Test
        fun `getting a user by id when id exists, returns a 200 response`() {

            //Arrange - add the user
            val addResponse = addUser(validName, validEmail)
            val addedUser : User = jsonToObject(addResponse.body.toString())

            //Assert - retrieve the added user from the database and verify return code
            val retrieveResponse = retrieveUserById(addedUser.id)
            assertEquals(200, retrieveResponse.status)

            //After - restore the db to previous state by deleting the added user
            deleteUser(addedUser.id)
        }

        @Test
        fun `getting a user by email when email exists, returns a 200 response`() {

            //Arrange - add the user
            addUser(validName, validEmail)

            //Assert - retrieve the added user from the database and verify return code
            val retrieveResponse = retrieveUserByEmail(validEmail)
            assertEquals(200, retrieveResponse.status)

            //After - restore the db to previous state by deleting the added user
            val retrievedUser : User = jsonToObject(retrieveResponse.body.toString())
            deleteUser(retrievedUser.id)
        }

    }

    @Nested
    inner class UpdateUsers {
        @Test
        fun `updating a user when it exists, returns a 204 response`() {

            //Arrange - add the user that we plan to do an update on
            val updatedName = "Updated Name"
            val updatedEmail = "Updated Email"
            val addedResponse = addUser(validName, validEmail)
            val addedUser : User = jsonToObject(addedResponse.body.toString())

            //Act & Assert - update the email and name of the retrieved user and assert 204 is returned
            assertEquals(204, updateUser(addedUser.id, updatedName, updatedEmail).status)

            //Act & Assert - retrieve updated user and assert details are correct
            val updatedUserResponse = retrieveUserById(addedUser.id)
            val updatedUser : User = jsonToObject(updatedUserResponse.body.toString())
            assertEquals(updatedName, updatedUser.name)
            assertEquals(updatedEmail, updatedUser.email)

            //After - restore the db to previous state by deleting the added user
            deleteUser(addedUser.id)
        }

        @Test
        fun `updating a user when it doesn't exist, returns a 404 response`() {

            //Arrange - creating some text fixture data
            val updatedName = "Updated Name"
            val updatedEmail = "Updated Email"

            //Act & Assert - attempt to update the email and name of user that doesn't exist
            assertEquals(404, updateUser(-1, updatedName, updatedEmail).status)
        }

    }

    @Nested
    inner class DeleteUsers {
        @Test
        fun `deleting a user when it doesn't exist, returns a 404 response`() {
            //Act & Assert - attempt to delete a user that doesn't exist
            assertEquals(404, deleteUser(-1).status)
        }

        @Test
        fun `deleting a user when it exists, returns a 204 response`() {

            //Arrange - add the user that we plan to do a delete on
            val addedResponse = addUser(validName, validEmail)
            val addedUser : User = jsonToObject(addedResponse.body.toString())

            //Act & Assert - delete the added user and assert a 204 is returned
            assertEquals(204, deleteUser(addedUser.id).status)

            //Act & Assert - attempt to retrieve the deleted user --> 404 response
            assertEquals(404, retrieveUserById(addedUser.id).status)
        }


    }



    @Nested
    inner class CreateActivities {

        @Test
        fun `add an activity when a user exists for it, returns a 201 response`() {

            //Arrange - add a user and an associated activity that we plan to do a delete on
            val addedUser: User = jsonToObject(addUser(validName, validEmail).body.toString())

            val addActivityResponse = addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id
            )
            assertEquals(201, addActivityResponse.status)

            //After - delete the user (Activity will cascade delete in the database)
            deleteUser(addedUser.id)
        }

        @Test
        fun `add an activity when no user exists for it, returns a 404 response`() {

            //Arrange - check there is no user for -1 id
            val userId = -1
            assertEquals(404, retrieveUserById(userId).status)

            val addActivityResponse = addActivity(
                activities.get(0).description, activities.get(0).duration,
                activities.get(0).calories, activities.get(0).started, userId
            )
            assertEquals(404, addActivityResponse.status)
        }
    }

    @Nested
    inner class CreateMapActivities {

        @Test
        fun `add a map activity when a user exists returns a 201 response`() {
            val addedUser: User = jsonToObject(addUser(validName, validEmail).body.toString())

            val addActivityResponse = addMapActivity(
                userId = addedUser.id,
                startLat = 53.3498,
                startLng = -6.2603,
                endLat = 53.3438,
                endLng = -6.2546
            )

            assertEquals(201, addActivityResponse.status)
            val addedActivity = jsonNodeToObject<Activity>(addActivityResponse)
            assertEquals(addedUser.id, addedActivity.userId)
            assertNotEquals(0.0, addedActivity.distanceKm)
            assertEquals(true, addedActivity.description.startsWith("From "))

            deleteUser(addedUser.id)
        }

        @Test
        fun `add a map activity when user does not exist returns a 404 response`() {
            val addActivityResponse = addMapActivity(
                userId = -1,
                startLat = 53.3498,
                startLng = -6.2603,
                endLat = 53.3438,
                endLng = -6.2546
            )

            assertEquals(404, addActivityResponse.status)
        }

        @Test
        fun `map activity uses fallback names when reverse geocode fails`() {
            val addedUser: User = jsonToObject(addUser(validName, validEmail).body.toString())

            val response = addMapActivity(
                userId = addedUser.id,
                startLat = 0.0,
                startLng = 0.0,
                endLat = 0.0,
                endLng = 0.1
            )

            assertEquals(201, response.status)
            val activity = jsonNodeToObject<Activity>(response)
            assertEquals(true, activity.description.contains("Start (0.0, 0.0)"))
            assertEquals(true, activity.description.contains("End (0.0, 0.1)"))

            deleteUser(addedUser.id)
        }
    }

    @Nested
    inner class ReadActivities {

        @Test
        fun `get all activities returns 404 when none exist`() {
            val response = retrieveAllActivities()
            assertEquals(404, response.status)
        }

        @Test
        fun `get all activities returns 200 when data exists`() {
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id)

            val response = retrieveAllActivities()
            assertEquals(200, response.status)
            val retrievedActivities = jsonNodeToObject<Array<Activity>>(response)
            assertNotEquals(0, retrievedActivities.size)
        }

        @Test
        fun `get all activities by user id when user and activities exists returns 200 response`() {
            //Arrange - add a user and 3 associated activities that we plan to retrieve
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id)
            addActivity(
                activities[1].description, activities[1].duration,
                activities[1].calories, activities[1].started, addedUser.id)
            addActivity(
                activities[2].description, activities[2].duration,
                activities[2].calories, activities[2].started, addedUser.id)

            //Assert and Act - retrieve the three added activities by user id
            val response = retrieveActivitiesByUserId(addedUser.id)
            assertEquals(200, response.status)
            val retrievedActivities = jsonNodeToObject<Array<Activity>>(response)
            assertEquals(3, retrievedActivities.size)

            //After - delete the added user and assert a 204 is returned (activities are cascade deleted)
            assertEquals(204, deleteUser(addedUser.id).status)
        }

        @Test
        fun `get all activities by user id when no activities exist returns 404 response`() {
            //Arrange - add a user
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())

            //Assert and Act - retrieve the activities by user id
            val response = retrieveActivitiesByUserId(addedUser.id)
            assertEquals(404, response.status)

            //After - delete the added user and assert a 204 is returned
            assertEquals(204, deleteUser(addedUser.id).status)
        }

        @Test
        fun `get all activities by user id when no user exists returns 404 response`() {
            //Arrange
            val userId = -1

            //Assert and Act - retrieve activities by user id
            val response = retrieveActivitiesByUserId(userId)
            assertEquals(404, response.status)
        }

        @Test
        fun `get activity by activity id when no activity exists returns 404 response`() {
            //Arrange
            val activityId = -1
            //Assert and Act - attempt to retrieve the activity by activity id
            val response = retrieveActivityByActivityId(activityId)
            assertEquals(404, response.status)
        }


        @Test
        fun `get activity by activity id when activity exists returns 200 response`() {
            //Arrange - add a user and associated activity
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            val addActivityResponse = addActivity(
                activities[0].description,
                activities[0].duration, activities[0].calories,
                activities[0].started, addedUser.id)
            assertEquals(201, addActivityResponse.status)
            val addedActivity = jsonNodeToObject<Activity>(addActivityResponse)

            //Act & Assert - retrieve the activity by activity id
            val response = retrieveActivityByActivityId(addedActivity.id)
            assertEquals(200, response.status)

            //After - delete the added user and assert a 204 is returned
            assertEquals(204, deleteUser(addedUser.id).status)
        }

    }

    @Nested
    inner class UpdateActivities {

        @Test
        fun `updating an activity by activity id when it doesn't exist, returns a 404 response`() {
            val userId = -1
            val activityID = -1

            //Arrange - check there is no user for -1 id
            assertEquals(404, retrieveUserById(userId).status)

            //Act & Assert - attempt to update the details of an activity/user that doesn't exist
            assertEquals(
                404, updateActivity(
                    activityID, updatedDescription, updatedDuration,
                    updatedCalories, updatedStarted, userId
                ).status
            )
        }

        @Test
        fun `updating an activity by activity id when it exists, returns 204 response`() {

            //Arrange - add a user and an associated activity that we plan to do an update on
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            val addActivityResponse = addActivity(
                activities[0].description,
                activities[0].duration, activities[0].calories,
                activities[0].started, addedUser.id)
            assertEquals(201, addActivityResponse.status)
            val addedActivity = jsonNodeToObject<Activity>(addActivityResponse)

            //Act & Assert - update the added activity and assert a 204 is returned
            val updatedActivityResponse = updateActivity(addedActivity.id, updatedDescription,
                updatedDuration, updatedCalories, updatedStarted, addedUser.id)
            assertEquals(204, updatedActivityResponse.status)

            //Assert that the individual fields were all updated as expected
            val retrievedActivityResponse = retrieveActivityByActivityId(addedActivity.id)
            val updatedActivity = jsonNodeToObject<Activity>(retrievedActivityResponse)
            assertEquals(updatedDescription,updatedActivity.description)
            assertEquals(updatedDuration, updatedActivity.duration, 0.1)
            assertEquals(updatedCalories, updatedActivity.calories)
            assertEquals(updatedStarted, updatedActivity.started )

            //After - delete the user
            deleteUser(addedUser.id)
        }
    }

    @Nested
    inner class DeleteActivities {

        @Test
        fun `deleting an activity by activity id when it doesn't exist, returns a 404 response`() {
            //Act & Assert - attempt to delete a user that doesn't exist
            assertEquals(404, deleteActivityByActivityId(-1).status)
        }

        @Test
        fun `deleting activities by user id when it doesn't exist, returns a 404 response`() {
            //Act & Assert - attempt to delete a user that doesn't exist
            assertEquals(404, deleteActivitiesByUserId(-1).status)
        }

        @Test
        fun `deleting an activity without admin role returns 403 response`() {
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            val addActivityResponse = addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id)
            val addedActivity = jsonNodeToObject<Activity>(addActivityResponse)

            assertEquals(403, deleteActivityByActivityId(addedActivity.id, role = "user").status)
            deleteUser(addedUser.id)
        }

        @Test
        fun `deleting activities by user id without admin role returns 403 response`() {
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id)

            assertEquals(403, deleteActivitiesByUserId(addedUser.id, role = "user").status)
            deleteUser(addedUser.id)
        }

        @Test
        fun `deleting an activity by id when it exists, returns a 204 response`() {

            //Arrange - add a user and an associated activity that we plan to do a delete on
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            val addActivityResponse = addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id)
            assertEquals(201, addActivityResponse.status)

            //Act & Assert - delete the added activity and assert a 204 is returned
            val addedActivity = jsonNodeToObject<Activity>(addActivityResponse)
            assertEquals(204, deleteActivityByActivityId(addedActivity.id).status)

            //After - delete the user
            deleteUser(addedUser.id)
        }

        @Test
        fun `deleting all activities by userid when it exists, returns a 204 response`() {

            //Arrange - add a user and 3 associated activities that we plan to do a cascade delete
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            val addActivityResponse1 = addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id)
            assertEquals(201, addActivityResponse1.status)
            val addActivityResponse2 = addActivity(
                activities[1].description, activities[1].duration,
                activities[1].calories, activities[1].started, addedUser.id)
            assertEquals(201, addActivityResponse2.status)
            val addActivityResponse3 = addActivity(
                activities[2].description, activities[2].duration,
                activities[2].calories, activities[2].started, addedUser.id)
            assertEquals(201, addActivityResponse3.status)

            //Act & Assert - delete the added user and assert a 204 is returned
            assertEquals(204, deleteUser(addedUser.id).status)

            //Act & Assert - attempt to retrieve the deleted activities
            val addedActivity1 = jsonNodeToObject<Activity>(addActivityResponse1)
            val addedActivity2 = jsonNodeToObject<Activity>(addActivityResponse2)
            val addedActivity3 = jsonNodeToObject<Activity>(addActivityResponse3)
            assertEquals(404, retrieveActivityByActivityId(addedActivity1.id).status)
            assertEquals(404, retrieveActivityByActivityId(addedActivity2.id).status)
            assertEquals(404, retrieveActivityByActivityId(addedActivity3.id).status)
        }
    }

    @Nested
    inner class MilestoneAdminAccess {

        @Test
        fun `adding a milestone without admin role returns 403 response`() {
            val response = addMilestone(
                name = "Rivendell",
                description = "Elven refuge",
                targetSteps = 250000
            )
            assertEquals(403, response.status)
        }

        @Test
        fun `adding a milestone with admin role returns 201 response`() {
            val response = addMilestone(
                name = "Moria",
                description = "Dwarven halls",
                targetSteps = 400000,
                role = "admin"
            )
            assertEquals(201, response.status)
            val addedMilestone = jsonNodeToObject<Milestone>(response)
            assertEquals(204, deleteMilestone(addedMilestone.id, role = "admin").status)
        }

        @Test
        fun `updating a milestone without admin role returns 403 response`() {
            val createResponse = addMilestone(
                name = "Lothlorien",
                description = "Golden wood",
                targetSteps = 500000,
                role = "admin"
            )
            val addedMilestone = jsonNodeToObject<Milestone>(createResponse)

            val updateResponse = updateMilestone(
                id = addedMilestone.id,
                name = "Lothlorien",
                description = "Golden wood updated",
                targetSteps = 510000
            )
            assertEquals(403, updateResponse.status)
            assertEquals(204, deleteMilestone(addedMilestone.id, role = "admin").status)
        }

        @Test
        fun `deleting a milestone without admin role returns 403 response`() {
            val createResponse = addMilestone(
                name = "Bree",
                description = "Safe stop",
                targetSteps = 150000,
                role = "admin"
            )
            val addedMilestone = jsonNodeToObject<Milestone>(createResponse)

            val deleteResponse = deleteMilestone(addedMilestone.id)
            assertEquals(403, deleteResponse.status)
            assertEquals(204, deleteMilestone(addedMilestone.id, role = "admin").status)
        }
    }

    @Nested
    inner class UserActivityAdminAccess {

        @Test
        fun `adding a user without admin role returns 403 response`() {
            val response = addUser("Samwise Gamgee", "samwise@gondor.com", role = "user")
            assertEquals(403, response.status)
        }

        @Test
        fun `deleting an activity without admin role returns 403 response`() {
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            val addActivityResponse = addActivity(
                activities[0].description,
                activities[0].duration,
                activities[0].calories,
                activities[0].started,
                addedUser.id
            )
            val addedActivity = jsonNodeToObject<Activity>(addActivityResponse)

            val deleteResponse = deleteActivityByActivityId(addedActivity.id, role = "user")
            assertEquals(403, deleteResponse.status)

            deleteUser(addedUser.id)
        }
    }

    @Nested
    inner class AchievementAdminAccess {

        @Test
        fun `adding an achievement without admin role returns 403 response`() {
            val badgeFile = createTempBadge()
            val response = addAchievement(
                name = "First Kilometer",
                description = "Completed a kilometer",
                targetDistanceKm = 1.0,
                badgeFile = badgeFile,
                role = "user"
            )
            assertEquals(403, response.status)
        }

        @Test
        fun `updating an achievement without admin role returns 403 response`() {
            val createResponse = addAchievement(
                name = "First Kilometer",
                description = "Completed a kilometer",
                targetDistanceKm = 1.0,
                badgeFile = createTempBadge(),
                role = "admin"
            )
            val created = jsonNodeToObject<Achievement>(createResponse)

            val updateResponse = updateAchievement(
                id = created.id,
                name = "Updated",
                description = "Updated description",
                targetDistanceKm = 1.5,
                badgeFile = null,
                role = "user"
            )
            assertEquals(403, updateResponse.status)
        }

        @Test
        fun `earned achievements returned when distance meets target`() {
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            addActivity(
                description = "Evening walk",
                duration = 30.0,
                calories = 120,
                started = DateTime.now(),
                userId = addedUser.id,
                steps = 2000,
                distanceKm = 3.2
            )

            val badgeFile = createTempBadge()
            addAchievement(
                name = "First Kilometer",
                description = "Completed a kilometer",
                targetDistanceKm = 1.0,
                badgeFile = badgeFile,
                role = "admin"
            )
            addAchievement(
                name = "Ten K",
                description = "Completed ten kilometers",
                targetDistanceKm = 10.0,
                badgeFile = badgeFile,
                role = "admin"
            )

            val response = retrieveAchievementsByUserId(addedUser.id)
            assertEquals(200, response.status)
            val achievements = jsonNodeToObject<Array<Achievement>>(response)
            assertEquals(1, achievements.size)
            assertEquals("First Kilometer", achievements[0].name)
        }
    }

    @Nested
    inner class AdminRestrictionResponses {

        @Test
        fun `updating a user without admin role returns 403 response`() {
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            val response = updateUser(addedUser.id, "Updated Name", "updated@test.com", role = "user")
            assertEquals(403, response.status)
        }

        @Test
        fun `deleting a user without admin role returns 403 response`() {
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            val response = deleteUser(addedUser.id, role = "user")
            assertEquals(403, response.status)
        }

        @Test
        fun `updating an activity without admin role returns 403 response`() {
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            val addActivityResponse = addActivity(
                activities[0].description,
                activities[0].duration,
                activities[0].calories,
                activities[0].started,
                addedUser.id
            )
            val addedActivity = jsonNodeToObject<Activity>(addActivityResponse)

            val response = updateActivity(
                addedActivity.id,
                "Updated Description",
                20.0,
                200,
                DateTime.now(),
                addedUser.id,
                role = "user"
            )
            assertEquals(403, response.status)
        }

        @Test
        fun `deleting activities by user id returns 204 when admin`() {
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            addActivity(
                activities[0].description,
                activities[0].duration,
                activities[0].calories,
                activities[0].started,
                addedUser.id
            )

            val response = deleteActivitiesByUserId(addedUser.id, role = "admin")
            assertEquals(204, response.status)
        }
    }

    @Nested
    inner class MilestoneControllerResponses {

        @Test
        fun `get all milestones returns 404 when none exist`() {
            val response = retrieveAllMilestones()
            assertEquals(404, response.status)
        }

        @Test
        fun `get all milestones returns 200 when data exists`() {
            addMilestone(
                name = "Rivendell",
                description = "Elven refuge",
                targetSteps = 300000,
                role = "admin"
            )

            val response = retrieveAllMilestones()
            assertEquals(200, response.status)
        }

        @Test
        fun `get milestone by id returns 404 when missing`() {
            val response = retrieveMilestoneById(999)
            assertEquals(404, response.status)
        }

        @Test
        fun `get milestone by id returns 200 when it exists`() {
            val createResponse = addMilestone(
                name = "Moria",
                description = "Dark halls",
                targetSteps = 700000,
                role = "admin"
            )
            val created = jsonNodeToObject<Milestone>(createResponse)

            val response = retrieveMilestoneById(created.id)
            assertEquals(200, response.status)
        }

        @Test
        fun `update milestone returns 404 when missing`() {
            val updateResponse = updateMilestone(
                id = 999,
                name = "Missing",
                description = "Missing",
                targetSteps = 123,
                role = "admin"
            )
            assertEquals(404, updateResponse.status)
        }

        @Test
        fun `update milestone returns 204 when admin`() {
            val createResponse = addMilestone(
                name = "Lothlorien",
                description = "Golden wood",
                targetSteps = 500000,
                role = "admin"
            )
            val created = jsonNodeToObject<Milestone>(createResponse)

            val updateResponse = updateMilestone(
                id = created.id,
                name = "Lothlorien",
                description = "Golden wood updated",
                targetSteps = 510000,
                role = "admin"
            )
            assertEquals(204, updateResponse.status)
        }

        @Test
        fun `delete milestone returns 404 when missing`() {
            val deleteResponse = deleteMilestone(999, role = "admin")
            assertEquals(404, deleteResponse.status)
        }

        @Test
        fun `delete milestone returns 204 when admin`() {
            val createResponse = addMilestone(
                name = "Bree",
                description = "Safe stop",
                targetSteps = 150000,
                role = "admin"
            )
            val created = jsonNodeToObject<Milestone>(createResponse)

            val deleteResponse = deleteMilestone(created.id, role = "admin")
            assertEquals(204, deleteResponse.status)
        }
    }

    @Nested
    inner class AchievementControllerResponses {

        @Test
        fun `get all achievements returns 404 when none exist`() {
            val response = retrieveAllAchievements()
            assertEquals(404, response.status)
        }

        @Test
        fun `get all achievements returns 200 when data exists`() {
            addAchievement(
                name = "First Kilometer",
                description = "Completed a kilometer",
                targetDistanceKm = 1.0,
                badgeFile = createTempBadge(),
                role = "admin"
            )

            val response = retrieveAllAchievements()
            assertEquals(200, response.status)
        }

        @Test
        fun `get achievement by id returns 200 when it exists`() {
            val createResponse = addAchievement(
                name = "First Kilometer",
                description = "Completed a kilometer",
                targetDistanceKm = 1.0,
                badgeFile = createTempBadge(),
                role = "admin"
            )
            val created = jsonNodeToObject<Achievement>(createResponse)

            val response = retrieveAchievementById(created.id)
            assertEquals(200, response.status)
        }

        @Test
        fun `get achievement by id returns 404 when missing`() {
            val response = retrieveAchievementById(999)
            assertEquals(404, response.status)
        }

        @Test
        fun `get achievements by user id returns 404 when user missing`() {
            val response = retrieveAchievementsByUserId(999)
            assertEquals(404, response.status)
        }

        @Test
        fun `get achievements by user id returns 404 when none earned`() {
            val addedUser : User = jsonToObject(addUser(validName, validEmail).body.toString())
            addAchievement(
                name = "First Kilometer",
                description = "Completed a kilometer",
                targetDistanceKm = 1.0,
                badgeFile = createTempBadge(),
                role = "admin"
            )

            val response = retrieveAchievementsByUserId(addedUser.id)
            assertEquals(404, response.status)
        }

        @Test
        fun `adding achievement with missing fields returns 400 response`() {
            val request = Unirest.post(origin + "/api/achievements")
                .field("name", "Missing Badge")
                .field("description", "No badge uploaded")
                .field("targetDistanceKm", "1.0")
                .header("X-User-Role", "admin")
            val response = request.asJson()
            assertEquals(400, response.status)
        }

        @Test
        fun `adding achievement with blank name returns 400 response`() {
            val request = Unirest.post(origin + "/api/achievements")
                .field("name", "")
                .field("description", "Blank name")
                .field("targetDistanceKm", "1.0")
                .field("badge", createTempBadge())
                .header("X-User-Role", "admin")
            val response = request.asJson()
            assertEquals(400, response.status)
        }

        @Test
        fun `adding achievement with missing name returns 400 response`() {
            val request = Unirest.post(origin + "/api/achievements")
                .field("description", "Missing name")
                .field("targetDistanceKm", "1.0")
                .field("badge", createTempBadge())
                .header("X-User-Role", "admin")
            val response = request.asJson()
            assertEquals(400, response.status)
        }

        @Test
        fun `adding achievement with blank description returns 400 response`() {
            val request = Unirest.post(origin + "/api/achievements")
                .field("name", "Blank description")
                .field("description", "")
                .field("targetDistanceKm", "1.0")
                .field("badge", createTempBadge())
                .header("X-User-Role", "admin")
            val response = request.asJson()
            assertEquals(400, response.status)
        }

        @Test
        fun `adding achievement with missing description returns 400 response`() {
            val request = Unirest.post(origin + "/api/achievements")
                .field("name", "Missing description")
                .field("targetDistanceKm", "1.0")
                .field("badge", createTempBadge())
                .header("X-User-Role", "admin")
            val response = request.asJson()
            assertEquals(400, response.status)
        }

        @Test
        fun `adding achievement with missing target distance returns 400 response`() {
            val request = Unirest.post(origin + "/api/achievements")
                .field("name", "Missing target")
                .field("description", "Missing target distance")
                .field("badge", createTempBadge())
                .header("X-User-Role", "admin")
            val response = request.asJson()
            assertEquals(400, response.status)
        }

        @Test
        fun `adding achievement with invalid target distance returns 400 response`() {
            val request = Unirest.post(origin + "/api/achievements")
                .field("name", "Invalid target")
                .field("description", "Bad target distance")
                .field("targetDistanceKm", "bad")
                .field("badge", createTempBadge())
                .header("X-User-Role", "admin")
            val response = request.asJson()
            assertEquals(400, response.status)
        }

        @Test
        fun `updating missing achievement returns 404 response`() {
            val response = updateAchievement(
                id = 999,
                name = "Missing",
                description = "Missing",
                targetDistanceKm = 1.0,
                badgeFile = null,
                role = "admin"
            )
            assertEquals(404, response.status)
        }

        @Test
        fun `updating achievement without badge keeps existing badge`() {
            val createResponse = addAchievement(
                name = "First Kilometer",
                description = "Completed a kilometer",
                targetDistanceKm = 1.0,
                badgeFile = createTempBadge(),
                role = "admin"
            )
            val created = jsonNodeToObject<Achievement>(createResponse)

            val updateResponse = updateAchievement(
                id = created.id,
                name = "First Kilometer",
                description = "Updated description",
                targetDistanceKm = 1.2,
                badgeFile = null,
                role = "admin"
            )
            assertEquals(204, updateResponse.status)

            val retrieved = jsonNodeToObject<Achievement>(retrieveAchievementById(created.id))
            assertEquals(created.badgePath, retrieved.badgePath)
        }

        @Test
        fun `updating achievement with new badge replaces badge path`() {
            val createResponse = addAchievement(
                name = "First Kilometer",
                description = "Completed a kilometer",
                targetDistanceKm = 1.0,
                badgeFile = createTempBadge(),
                role = "admin"
            )
            val created = jsonNodeToObject<Achievement>(createResponse)

            val updateResponse = updateAchievement(
                id = created.id,
                name = "First Kilometer",
                description = "Updated again",
                targetDistanceKm = 1.5,
                badgeFile = createTempBadge(),
                role = "admin"
            )
            assertEquals(204, updateResponse.status)

            val retrieved = jsonNodeToObject<Achievement>(retrieveAchievementById(created.id))
            assertNotEquals(created.badgePath, retrieved.badgePath)
        }

        @Test
        fun `updating achievement with missing fields keeps existing values`() {
            val createResponse = addAchievement(
                name = "First Kilometer",
                description = "Completed a kilometer",
                targetDistanceKm = 1.0,
                badgeFile = createTempBadge(),
                role = "admin"
            )
            val created = jsonNodeToObject<Achievement>(createResponse)

            val request = Unirest.patch(origin + "/api/achievements/${created.id}")
                .field("badge", createTempBadge())
                .header("X-User-Role", "admin")
            val response = request.asJson()
            assertEquals(204, response.status)

            val retrieved = jsonNodeToObject<Achievement>(retrieveAchievementById(created.id))
            assertEquals(created.name, retrieved.name)
            assertEquals(created.description, retrieved.description)
            assertEquals(created.targetDistanceKm, retrieved.targetDistanceKm, 0.01)
        }

        @Test
        fun `updating achievement with invalid target distance keeps existing value`() {
            val createResponse = addAchievement(
                name = "First Kilometer",
                description = "Completed a kilometer",
                targetDistanceKm = 1.0,
                badgeFile = createTempBadge(),
                role = "admin"
            )
            val created = jsonNodeToObject<Achievement>(createResponse)

            val request = Unirest.patch(origin + "/api/achievements/${created.id}")
                .field("name", created.name)
                .field("description", created.description)
                .field("targetDistanceKm", "bad")
                .header("X-User-Role", "admin")
            val response = request.asJson()
            assertEquals(204, response.status)

            val retrieved = jsonNodeToObject<Achievement>(retrieveAchievementById(created.id))
            assertEquals(created.targetDistanceKm, retrieved.targetDistanceKm, 0.01)
        }

        @Test
        fun `deleting achievement returns 204 when admin`() {
            val createResponse = addAchievement(
                name = "First Kilometer",
                description = "Completed a kilometer",
                targetDistanceKm = 1.0,
                badgeFile = createTempBadge(),
                role = "admin"
            )
            val created = jsonNodeToObject<Achievement>(createResponse)

            val response = deleteAchievement(created.id, role = "admin")
            assertEquals(204, response.status)
        }

        @Test
        fun `deleting missing achievement returns 404 when admin`() {
            val response = deleteAchievement(999, role = "admin")
            assertEquals(404, response.status)
        }

        @Test
        fun `deleting achievement without admin returns 403 response`() {
            val createResponse = addAchievement(
                name = "First Kilometer",
                description = "Completed a kilometer",
                targetDistanceKm = 1.0,
                badgeFile = createTempBadge(),
                role = "admin"
            )
            val created = jsonNodeToObject<Achievement>(createResponse)

            val response = deleteAchievement(created.id, role = "user")
            assertEquals(403, response.status)
        }
    }

    //helper function to add a test user to the database
    private fun addUser (name: String, email: String, role: String? = "admin"): HttpResponse<JsonNode> {
        val request = Unirest.post(origin + "/api/users")
            .body("{\"name\":\"$name\", \"email\":\"$email\"}")
        if (role != null) {
            request.header("X-User-Role", role)
        }
        return request.asJson()
    }

    //helper function to delete a test user from the database
    private fun deleteUser (id: Int, role: String? = "admin"): HttpResponse<String> {
        val request = Unirest.delete(origin + "/api/users/$id")
        if (role != null) {
            request.header("X-User-Role", role)
        }
        return request.asString()
    }

    //helper function to retrieve a test user from the database by email
    private fun retrieveUserByEmail(email : String) : HttpResponse<String> {
        return Unirest.get(origin + "/api/users/email/${email}").asString()
    }

    //helper function to retrieve a test user from the database by id
    private fun retrieveUserById(id: Int) : HttpResponse<String> {
        return Unirest.get(origin + "/api/users/${id}").asString()
    }

    //helper function to add a test user to the database
    private fun updateUser (id: Int, name: String, email: String, role: String? = "admin"): HttpResponse<JsonNode> {
        val request = Unirest.patch(origin + "/api/users/$id")
            .body("{\"name\":\"$name\", \"email\":\"$email\"}")
        if (role != null) {
            request.header("X-User-Role", role)
        }
        return request.asJson()
    }

    //helper function to retrieve all activities
    private fun retrieveAllActivities(): HttpResponse<JsonNode> {
        return Unirest.get(origin + "/api/activities").asJson()
    }

    //helper function to retrieve activities by user id
    private fun retrieveActivitiesByUserId(id: Int): HttpResponse<JsonNode> {
        return Unirest.get(origin + "/api/users/${id}/activities").asJson()
    }

    //helper function to retrieve activity by activity id
    private fun retrieveActivityByActivityId(id: Int): HttpResponse<JsonNode> {
        return Unirest.get(origin + "/api/activities/${id}").asJson()
    }

    //helper function to delete an activity by activity id
    private fun deleteActivityByActivityId(id: Int, role: String? = "admin"): HttpResponse<String> {
        val request = Unirest.delete(origin + "/api/activities/$id")
        if (role != null) {
            request.header("X-User-Role", role)
        }
        return request.asString()
    }

    //helper function to delete an activity by activity id
    private fun deleteActivitiesByUserId(id: Int, role: String? = "admin"): HttpResponse<String> {
        val request = Unirest.delete(origin + "/api/users/$id/activities")
        if (role != null) {
            request.header("X-User-Role", role)
        }
        return request.asString()
    }

    //helper function to add a test user to the database
    private fun updateActivity(id: Int, description: String, duration: Double, calories: Int,
                               started: DateTime, userId: Int, role: String? = "admin"): HttpResponse<JsonNode> {
        val request = Unirest.patch(origin + "/api/activities/$id")
            .body("""
                {
                  "description":"$description",
                  "duration":$duration,
                  "calories":$calories,
                  "started":"$started",
                  "userId":$userId
                }
            """.trimIndent())
        if (role != null) {
            request.header("X-User-Role", role)
        }
        return request.asJson()
    }

    private fun addMapActivity(
        userId: Int,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): HttpResponse<JsonNode> {
        return Unirest.post(origin + "/api/users/$userId/activities/map")
            .body("""
                {
                  "startLat":$startLat,
                  "startLng":$startLng,
                  "endLat":$endLat,
                  "endLng":$endLng
                }
            """.trimIndent())
            .asJson()
    }

    //helper function to add an activity
    private fun addActivity(description: String, duration: Double, calories: Int,
                            started: DateTime, userId: Int, steps: Int = 0,
                            distanceKm: Double = 0.0): HttpResponse<JsonNode> {
        return Unirest.post(origin + "/api/activities")
            .body("""
                {
                   "description":"$description",
                   "duration":$duration,
                   "calories":$calories,
                   "started":"$started",
                   "userId":$userId,
                   "steps":$steps,
                   "distanceKm":$distanceKm
                }
            """.trimIndent())
            .asJson()
    }

    private fun createTempBadge(): File {
        val tempFile = Files.createTempFile("badge", ".png").toFile()
        tempFile.writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
        tempFile.deleteOnExit()
        return tempFile
    }

    private fun addAchievement(
        name: String,
        description: String,
        targetDistanceKm: Double,
        badgeFile: File,
        role: String? = null
    ): HttpResponse<JsonNode> {
        val request = Unirest.post(origin + "/api/achievements")
            .field("name", name)
            .field("description", description)
            .field("targetDistanceKm", targetDistanceKm.toString())
            .field("badge", badgeFile)
        if (role != null) {
            request.header("X-User-Role", role)
        }
        return request.asJson()
    }

    private fun retrieveAchievementsByUserId(id: Int): HttpResponse<JsonNode> {
        return Unirest.get(origin + "/api/users/${id}/achievements").asJson()
    }

    private fun addMilestone(
        name: String,
        description: String,
        targetSteps: Int,
        role: String? = null
    ): HttpResponse<JsonNode> {
        val request = Unirest.post(origin + "/api/milestones")
            .body("""
                {
                  "name":"$name",
                  "description":"$description",
                  "targetSteps":$targetSteps
                }
            """.trimIndent())
        if (role != null) {
            request.header("X-User-Role", role)
        }
        return request.asJson()
    }

    private fun updateMilestone(
        id: Int,
        name: String,
        description: String,
        targetSteps: Int,
        role: String? = null
    ): HttpResponse<JsonNode> {
        val request = Unirest.patch(origin + "/api/milestones/$id")
            .body("""
                {
                  "name":"$name",
                  "description":"$description",
                  "targetSteps":$targetSteps
                }
            """.trimIndent())
        if (role != null) {
            request.header("X-User-Role", role)
        }
        return request.asJson()
    }

    private fun deleteMilestone(id: Int, role: String? = null): HttpResponse<String> {
        val request = Unirest.delete(origin + "/api/milestones/$id")
        if (role != null) {
            request.header("X-User-Role", role)
        }
        return request.asString()
    }

    private fun retrieveAllMilestones(): HttpResponse<JsonNode> {
        return Unirest.get(origin + "/api/milestones").asJson()
    }

    private fun retrieveMilestoneById(id: Int): HttpResponse<JsonNode> {
        return Unirest.get(origin + "/api/milestones/$id").asJson()
    }

    private fun retrieveAllAchievements(): HttpResponse<JsonNode> {
        return Unirest.get(origin + "/api/achievements").asJson()
    }

    private fun retrieveAchievementById(id: Int): HttpResponse<JsonNode> {
        return Unirest.get(origin + "/api/achievements/$id").asJson()
    }

    private fun updateAchievement(
        id: Int,
        name: String,
        description: String,
        targetDistanceKm: Double,
        badgeFile: File?,
        role: String? = null
    ): HttpResponse<JsonNode> {
        val request = Unirest.patch(origin + "/api/achievements/$id")
            .field("name", name)
            .field("description", description)
            .field("targetDistanceKm", targetDistanceKm.toString())
        if (badgeFile != null) {
            request.field("badge", badgeFile)
        }
        if (role != null) {
            request.header("X-User-Role", role)
        }
        return request.asJson()
    }

    private fun deleteAchievement(id: Int, role: String? = null): HttpResponse<String> {
        val request = Unirest.delete(origin + "/api/achievements/$id")
        if (role != null) {
            request.header("X-User-Role", role)
        }
        return request.asString()
    }


}
