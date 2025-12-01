package ie.setu.domain.repository

import ie.setu.domain.Milestone
import ie.setu.domain.db.Milestones
import ie.setu.utils.mapToMilestone
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * Data-access object for [Milestone] records.
 */
class MilestoneDAO {

    fun getAll(): ArrayList<Milestone> {
        val milestoneList: ArrayList<Milestone> = arrayListOf()
        transaction {
            Milestones.selectAll().map {
                milestoneList.add(mapToMilestone(it)) }
        }
        return milestoneList
    }

    fun findById(id: Int): Milestone?{
        return transaction {
            Milestones.selectAll().where { Milestones.id eq id }
                .map{mapToMilestone(it)}
                .firstOrNull()
        }
    }


    fun findByName(name: String) :Milestone?{
        return transaction {
            Milestones.selectAll().where { Milestones.name eq name}
                .map{mapToMilestone(it)}
                .firstOrNull()
        }
    }

    fun delete(id: Int):Int{
        return transaction{
            Milestones.deleteWhere{ Milestones.id eq id }
        }
    }

    /**
     * Adds a [milestone] to the Milestones table.
     * @return the id of the milestone following the add.
     */
    fun save(milestone: Milestone) : Int?{
        return transaction {
            Milestones.insert {
                it[name] = milestone.name
                it[description] = milestone.description
                it[targetSteps] = milestone.targetSteps
            } get Milestones.id
        }
    }

    fun update(id: Int, milestone: Milestone): Int{
        return transaction {
            Milestones.update ({
                Milestones.id eq id}) {
                it[name] = milestone.name
                it[description] = milestone.description
                it[targetSteps] = milestone.targetSteps
            }
        }
    }

}
