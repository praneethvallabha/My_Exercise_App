package com.recoverycoach.app.domain

import com.recoverycoach.app.data.PlanItem
import com.recoverycoach.app.data.RecoveryLevel

/**
 * Today's plan plus the reason it looks the way it does. The reason is part of
 * the output rather than something the UI invents, so the plan can never explain
 * itself differently from how it was built.
 */
data class DailyPlan(
    val level: RecoveryLevel,
    val items: List<PlanItem>,
    val rationale: String,
    val source: String?,
)

/**
 * Builds the day's plan from the recovery level.
 *
 * The three short post-meal walks survive every level. That is deliberate on two
 * counts: they are the highest-value thing in the plan for glucose control, and
 * keeping them means an easy or recovery day still registers as an active day,
 * so backing off does not quietly start a run of consecutive inactive days.
 *
 * Item ids are stable across levels so a tick made earlier in the day survives a
 * level change. An id that is not in today's plan is simply ignored.
 */
object PlanBuilder {

    private const val POST_MEAL_MINUTES = 10

    fun build(level: RecoveryLevel, doneIds: Set<String>): DailyPlan {
        val items = when (level) {
            RecoveryLevel.NORMAL -> listOf(
                item("morning", "Morning", "5.0 km deliberate walk", doneIds),
                postMeal("breakfast", "After breakfast", doneIds),
                postMeal("lunch", "After lunch", doneIds),
                postMeal("dinner", "After dinner", doneIds),
                item("evening", "Evening", "800 m swim, if comfortable", doneIds),
            )
            RecoveryLevel.EASY -> listOf(
                item("morning", "Morning", "3.0 km easy walk", doneIds),
                postMeal("breakfast", "After breakfast", doneIds),
                postMeal("lunch", "After lunch", doneIds),
                postMeal("dinner", "After dinner", doneIds),
                item("evening", "Evening", "400 m easy swim, only if it feels fine", doneIds),
            )
            RecoveryLevel.RECOVERY -> listOf(
                postMeal("breakfast", "After breakfast", doneIds),
                postMeal("lunch", "After lunch", doneIds),
                postMeal("dinner", "After dinner", doneIds),
            )
        }

        return DailyPlan(
            level = level,
            items = items,
            rationale = when (level) {
                RecoveryLevel.NORMAL ->
                    "Full plan. Nothing in your check-in asked for an easier day."
                RecoveryLevel.EASY ->
                    "Deliberate walk cut to 3 km and the swim halved. The short post-meal " +
                        "walks stay — they are the part that does most for glucose, and keeping " +
                        "them means today still counts as an active day."
                RecoveryLevel.RECOVERY ->
                    "Deliberate walk and swim are off today. The three short post-meal walks " +
                        "stay so this does not become a full rest day — guidance is to avoid " +
                        "more than two consecutive days without activity."
            },
            source = when (level) {
                RecoveryLevel.NORMAL -> null
                else -> "${TipSources.POSTMEAL_2013} · ${TipSources.ADA_2026}"
            },
        )
    }

    private fun item(id: String, whenLabel: String, title: String, doneIds: Set<String>) =
        PlanItem(id = id, whenLabel = whenLabel, title = title, done = id in doneIds)

    private fun postMeal(id: String, whenLabel: String, doneIds: Set<String>) =
        item(id, whenLabel, "$POST_MEAL_MINUTES min easy walk", doneIds)
}
