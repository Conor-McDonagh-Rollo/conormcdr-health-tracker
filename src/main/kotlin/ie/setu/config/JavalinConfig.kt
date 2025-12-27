package ie.setu.config

import ie.setu.controllers.HealthTrackerController
import ie.setu.utils.jsonObjectMapper
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.json.JavalinJackson
import io.javalin.vue.VueComponent
import java.io.File

/**
 * Configures and starts the Javalin HTTP server.
 *
 * It wires up JSON serialization, static file handling, the
 * Vue integration, and registers all REST and Vue routes.
 */
class JavalinConfig {

    /** The lazily configured Javalin instance used by the app and tests. */
    val app = Javalin.create(
        { config ->
            config.jsonMapper(JavalinJackson(jsonObjectMapper()))
            config.staticFiles.enableWebjars()
            val uploadsDir = File("uploads")
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs()
            }
            config.staticFiles.add {
                it.hostedPath = "/uploads"
                it.directory = uploadsDir.path
                it.location = Location.EXTERNAL
            }
            // Serve Vue components from the classpath so the packaged jar can find them
            config.vue.rootDirectory("/vue", Location.CLASSPATH)
            config.vue.vueInstanceNameInJs = "app"

        }
    ).apply {
        exception(Exception::class.java) { e, ctx -> e.printStackTrace() }
        error(404) { ctx -> ctx.json("404 - Not Found") }
    }

    /**
     * Starts the server on the assigned port and registers routes.
     */
    fun startJavalinService(): Javalin {
        app.start(getRemoteAssignedPort())
        registerRoutes(app)
        return app
    }

    /**
     * Registers REST endpoints and Vue routes on the provided [app].
     */
    private fun registerRoutes(app: Javalin) {
        //---------------
        // User API paths
        //---------------
        app.get("/api/users", HealthTrackerController::getAllUsers)
        app.get("/api/users/{user-id}", HealthTrackerController::getUserByUserId)
        app.post("/api/users", HealthTrackerController::addUser)
        app.delete("/api/users/{user-id}", HealthTrackerController::deleteUser)
        app.patch("/api/users/{user-id}", HealthTrackerController::updateUser)
        app.get("/api/users/email/{email}", HealthTrackerController::getUserByEmail)
        app.get("/api/users/{user-id}/activities", HealthTrackerController::getActivitiesByUserId)
        app.delete("/api/users/{user-id}/activities", HealthTrackerController::deleteActivityByUserId)
        app.post("/api/users/{user-id}/activities/map", HealthTrackerController::addActivityFromMap)

        //---------------------
        // Activities API paths
        //---------------------
        app.get("/api/activities", HealthTrackerController::getAllActivities)
        app.post("/api/activities", HealthTrackerController::addActivity)
        app.delete("/api/activities/{activity-id}", HealthTrackerController::deleteActivityByActivityId)
        app.patch("/api/activities/{activity-id}", HealthTrackerController::updateActivity)
        app.get("/api/activities/{activity-id}", HealthTrackerController::getActivitiesByActivityId)

        //---------------------
        // Milestones API paths
        //---------------------
        app.get("/api/milestones", HealthTrackerController::getAllMilestones)
        app.get("/api/milestones/{milestone-id}", HealthTrackerController::getMilestoneById)
        app.post("/api/milestones", HealthTrackerController::addMilestone)
        app.patch("/api/milestones/{milestone-id}", HealthTrackerController::updateMilestone)
        app.delete("/api/milestones/{milestone-id}", HealthTrackerController::deleteMilestone)

        //---------------------
        // Achievements API paths
        //---------------------
        app.get("/api/achievements", HealthTrackerController::getAllAchievements)
        app.get("/api/achievements/{achievement-id}", HealthTrackerController::getAchievementById)
        app.post("/api/achievements", HealthTrackerController::addAchievement)
        app.patch("/api/achievements/{achievement-id}", HealthTrackerController::updateAchievement)
        app.delete("/api/achievements/{achievement-id}", HealthTrackerController::deleteAchievement)
        app.get("/api/users/{user-id}/achievements", HealthTrackerController::getAchievementsByUserId)

        // The @routeComponent that we added in layout.html earlier will be replaced
        // by the String inside the VueComponent. This means a call to / will load
        // the layout and display our <home-page> component.
        app.get("/", VueComponent("<home-page></home-page>"))
        app.get("/users", VueComponent("<user-overview></user-overview>"))
        app.get("/users/{user-id}", VueComponent("<user-profile></user-profile>"))
        app.get("/users/{user-id}/activities", VueComponent("<user-activity-overview></user-activity-overview>"))
        app.get("/activities", VueComponent("<activity-overview></activity-overview>"))
        app.get("/milestones", VueComponent("<milestone-overview></milestone-overview>"))
        app.get("/achievements", VueComponent("<achievement-overview></achievement-overview>"))
    }

    /**
     * Returns the port to listen on, favouring the `PORT` environment
     * variable (useful on platforms like OpenShift) and falling back
     * to 8080 for local development.
     */
    private fun getRemoteAssignedPort(): Int {
        val remotePort = System.getenv("PORT")
        return if (remotePort != null) {
            Integer.parseInt(remotePort)
        } else 8080
    }

    /**
     * Returns the configured Javalin instance without starting it.
     *
     * Used primarily in tests that manage the lifecycle themselves.
     */
    fun getJavalinService(): Javalin {
        registerRoutes(app)
        return app
    }

}
