package ie.setu.controllers

import ie.setu.domain.Activity
import ie.setu.domain.ActivityMapRequest
import ie.setu.domain.Milestone
import ie.setu.domain.User
import ie.setu.domain.repository.ActivityDAO
import ie.setu.domain.repository.MilestoneDAO
import ie.setu.domain.repository.UserDAO
import ie.setu.utils.jsonToObject
import ie.setu.utils.OpenStreetMapService
import ie.setu.utils.estimateCalories
import ie.setu.utils.estimateDurationMinutes
import ie.setu.utils.estimateStepsFromDistanceKm
import ie.setu.utils.haversineDistanceKm
import io.javalin.http.Context
import org.joda.time.DateTime
import kotlin.math.roundToInt

/**
 * Javalin controller exposing REST endpoints for users, activities
 * and milestones.
 *
 * The functions here are referenced by [JavalinConfig] route
 * definitions and operate against the DAO layer.
 */
object HealthTrackerController {

    private val userDao = UserDAO()
    private val activityDAO = ActivityDAO()
    private val milestoneDAO = MilestoneDAO()

    /** Returns all users, or 404 if none exist. */
    fun getAllUsers(ctx: Context) {
        val users = userDao.getAll()
        if (users.size != 0) {
            ctx.status(200)
        }
        else{
            ctx.status(404)
        }
        ctx.json(users)
    }

    /** Returns a single user by `user-id` path parameter. */
    fun getUserByUserId(ctx: Context) {
        val user = userDao.findById(ctx.pathParam("user-id").toInt())
        if (user != null) {
            ctx.json(user)
            ctx.status(200)
        }
        else{
            ctx.status(404)
        }
    }

    /** Returns a single user identified by the `email` path parameter. */
    fun getUserByEmail(ctx: Context) {
        val user = userDao.findByEmail(ctx.pathParam("email"))
        if (user != null) {
            ctx.json(user)
            ctx.status(200)
        }
        else{
            ctx.status(404)
        }
    }

    /**
     * Creates a new user from the JSON request body and returns it
     * with its generated id and a 201 status code.
     */
    fun addUser(ctx: Context) {
        val user : User = jsonToObject(ctx.body())
        val userId = userDao.save(user)
        if (userId != null) {
            user.id = userId
            ctx.json(user)
            ctx.status(201)
        }
    }

    /** Deletes the user identified by `user-id`, returning 204 or 404. */
    fun deleteUser(ctx: Context){
        if (userDao.delete(ctx.pathParam("user-id").toInt()) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }

    /** Updates the user record identified by `user-id` using the JSON body. */
    fun updateUser(ctx: Context){
        val foundUser : User = jsonToObject(ctx.body())
        if ((userDao.update(id = ctx.pathParam("user-id").toInt(), user=foundUser)) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }

    //--------------------------------------------------------------
    // ActivityDAO specifics
    //-------------------------------------------------------------

    /** Returns all activities, or 404 if none exist. */
    fun getAllActivities(ctx: Context) {
        val activities = activityDAO.getAll()
        if (activities.size != 0) {
            ctx.status(200)
        }
        else{
            ctx.status(404)
        }
        ctx.json(activities)
    }

    /** Returns all activities for the user identified by `user-id`. */
    fun getActivitiesByUserId(ctx: Context) {
        if (userDao.findById(ctx.pathParam("user-id").toInt()) != null) {
            val activities = activityDAO.findByUserId(ctx.pathParam("user-id").toInt())
            if (activities.isNotEmpty()) {
                ctx.json(activities)
                ctx.status(200)
            }
            else{
                ctx.status(404)
            }
        }
        else{
            ctx.status(404)
        }
    }

    /** Returns a single activity identified by `activity-id`. */
    fun getActivitiesByActivityId(ctx: Context) {
        val activity = activityDAO.findByActivityId((ctx.pathParam("activity-id").toInt()))
        if (activity != null){
            ctx.json(activity)
            ctx.status(200)
        }
        else{
            ctx.status(404)
        }
    }

    /**
     * Creates a new activity linked to an existing user and returns it
     * with its generated id and a 201 status code.
     */
    fun addActivity(ctx: Context) {
        val activity : Activity = jsonToObject(ctx.body())
        val userId = userDao.findById(activity.userId)
        if (userId != null) {
            val activityId = activityDAO.save(activity)
            activity.id = activityId
            ctx.json(activity)
            ctx.status(201)
        }
        else{
            ctx.status(404)
        }
    }

    /**
     * Creates a new activity from two map points, auto-generating
     * the description and distance using OpenStreetMap.
     */
    fun addActivityFromMap(ctx: Context) {
        val userId = ctx.pathParam("user-id").toInt()
        if (userDao.findById(userId) == null) {
            ctx.status(404)
            return
        }

        val request: ActivityMapRequest = jsonToObject(ctx.body())
        val rawDistanceKm = haversineDistanceKm(
            request.startLat,
            request.startLng,
            request.endLat,
            request.endLng
        )
        val distanceKm = (rawDistanceKm * 100.0).roundToInt() / 100.0
        val startName = OpenStreetMapService.reverseGeocode(request.startLat, request.startLng)
            ?: "Start (${request.startLat}, ${request.startLng})"
        val endName = OpenStreetMapService.reverseGeocode(request.endLat, request.endLng)
            ?: "End (${request.endLat}, ${request.endLng})"
        val steps = estimateStepsFromDistanceKm(rawDistanceKm)

        val activity = Activity(
            id = 0,
            description = "From $startName to $endName",
            duration = estimateDurationMinutes(steps),
            calories = estimateCalories(steps),
            started = DateTime.now(),
            userId = userId,
            steps = steps,
            distanceKm = distanceKm
        )

        val activityId = activityDAO.save(activity)
        activity.id = activityId
        ctx.json(activity)
        ctx.status(201)
    }

    /** Deletes a single activity identified by `activity-id`. */
    fun deleteActivityByActivityId(ctx: Context){
        if (activityDAO.deleteByActivityId(ctx.pathParam("activity-id").toInt()) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }

    /** Deletes all activities for the user identified by `user-id`. */
    fun deleteActivityByUserId(ctx: Context){
        if (activityDAO.deleteByUserId(ctx.pathParam("user-id").toInt()) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }

    /** Updates an existing activity identified by `activity-id`. */
    fun updateActivity(ctx: Context){
        val activity : Activity = jsonToObject(ctx.body())
        if (activityDAO.updateByActivityId(
                activityId = ctx.pathParam("activity-id").toInt(),
                activityToUpdate = activity) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }

    //--------------------------------------------------------------
    // MilestoneDAO specifics
    //-------------------------------------------------------------
    /** Returns all milestones, or 404 if none exist. */
    fun getAllMilestones(ctx: Context) {
        val milestones = milestoneDAO.getAll()
        if (milestones.size != 0) {
            ctx.status(200)
        } else {
            ctx.status(404)
        }
        ctx.json(milestones)
    }

    /** Returns a single milestone identified by `milestone-id`. */
    fun getMilestoneById(ctx: Context) {
        val milestone = milestoneDAO.findById(ctx.pathParam("milestone-id").toInt())
        if (milestone != null) {
            ctx.json(milestone)
            ctx.status(200)
        } else {
            ctx.status(404)
        }
    }

    /**
     * Creates a new milestone from the JSON body and returns it with
     * its generated id and a 201 status code.
     */
    fun addMilestone(ctx: Context) {
        val milestone: Milestone = jsonToObject(ctx.body())
        val milestoneId = milestoneDAO.save(milestone)
        if (milestoneId != null) {
            milestone.id = milestoneId
            ctx.json(milestone)
            ctx.status(201)
        }
    }

    /** Deletes a milestone identified by `milestone-id`. */
    fun deleteMilestone(ctx: Context) {
        if (milestoneDAO.delete(ctx.pathParam("milestone-id").toInt()) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }

    /** Updates the milestone identified by `milestone-id`. */
    fun updateMilestone(ctx: Context) {
        val milestone: Milestone = jsonToObject(ctx.body())
        if ((milestoneDAO.update(id = ctx.pathParam("milestone-id").toInt(), milestone = milestone)) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }

}
