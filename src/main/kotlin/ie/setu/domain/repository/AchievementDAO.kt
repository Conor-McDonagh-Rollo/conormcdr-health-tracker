package ie.setu.domain.repository

import ie.setu.domain.Achievement
import ie.setu.domain.db.Achievements
import ie.setu.utils.mapToAchievement
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * Data-access object for [Achievement] records.
 */
class AchievementDAO {

    fun getAll(): ArrayList<Achievement> {
        val achievementList: ArrayList<Achievement> = arrayListOf()
        transaction {
            Achievements.selectAll()
                .orderBy(Achievements.targetDistanceKm to SortOrder.ASC)
                .map { achievementList.add(mapToAchievement(it)) }
        }
        return achievementList
    }

    fun findById(id: Int): Achievement? {
        return transaction {
            Achievements.selectAll().where { Achievements.id eq id }
                .map { mapToAchievement(it) }
                .firstOrNull()
        }
    }

    fun findByTargetDistance(maxDistanceKm: Double): List<Achievement> {
        return transaction {
            Achievements.selectAll()
                .where { Achievements.targetDistanceKm lessEq maxDistanceKm }
                .orderBy(Achievements.targetDistanceKm to SortOrder.ASC)
                .map { mapToAchievement(it) }
        }
    }

    fun delete(id: Int): Int {
        return transaction {
            Achievements.deleteWhere { Achievements.id eq id }
        }
    }

    fun save(achievement: Achievement): Int {
        return transaction {
            Achievements.insert {
                it[name] = achievement.name
                it[description] = achievement.description
                it[targetDistanceKm] = achievement.targetDistanceKm
                it[badgePath] = achievement.badgePath
            } get Achievements.id
        }
    }

    fun update(id: Int, achievement: Achievement): Int {
        return transaction {
            Achievements.update({
                Achievements.id eq id
            }) {
                it[name] = achievement.name
                it[description] = achievement.description
                it[targetDistanceKm] = achievement.targetDistanceKm
                it[badgePath] = achievement.badgePath
            }
        }
    }
}
