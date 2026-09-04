package com.recoverycoach.app.domain

import com.recoverycoach.app.data.RecoveryLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanBuilderTest {

    private fun ids(level: RecoveryLevel, done: Set<String> = emptySet()) =
        PlanBuilder.build(level, done).items.map { it.id }

    private val postMeal = setOf("breakfast", "lunch", "dinner")

    @Test
    fun `a normal day keeps the full plan`() {
        assertEquals(
            listOf("morning", "breakfast", "lunch", "dinner", "evening"),
            ids(RecoveryLevel.NORMAL),
        )
    }

    @Test
    fun `an easy day keeps every slot but scales the hard work down`() {
        val plan = PlanBuilder.build(RecoveryLevel.EASY, emptySet())
        assertEquals(listOf("morning", "breakfast", "lunch", "dinner", "evening"), plan.items.map { it.id })

        val walk = plan.items.first { it.id == "morning" }.title
        val swim = plan.items.first { it.id == "evening" }.title
        assertTrue("walk not reduced: $walk", walk.contains("3.0 km"))
        assertTrue("swim not reduced: $swim", swim.contains("400 m"))
    }

    @Test
    fun `a recovery day drops the deliberate walk and the swim`() {
        val plan = ids(RecoveryLevel.RECOVERY)
        assertFalse("deliberate walk still prescribed", plan.contains("morning"))
        assertFalse("swim still prescribed", plan.contains("evening"))
    }

    @Test
    fun `the recommendation never contradicts the plan`() {
        // The bug this fixes: a recovery day telling the user to stop the
        // deliberate walk while still listing a 5 km deliberate walk below it.
        val recovery = PlanBuilder.build(RecoveryLevel.RECOVERY, emptySet())
        assertFalse(recovery.items.any { it.title.contains("deliberate", ignoreCase = true) })
        assertFalse(recovery.items.any { it.title.contains("swim", ignoreCase = true) })
    }

    @Test
    fun `post-meal walks survive every level`() {
        RecoveryLevel.entries.forEach { level ->
            assertTrue("$level dropped a post-meal walk", ids(level).containsAll(postMeal))
        }
    }

    @Test
    fun `backing off never produces a day with nothing in it`() {
        // Keeping light movement is what stops an easy or recovery day starting a
        // run of consecutive inactive days.
        RecoveryLevel.entries.forEach { level ->
            assertTrue("$level has an empty plan", PlanBuilder.build(level, emptySet()).items.isNotEmpty())
        }
    }

    @Test
    fun `ticks survive a level change because ids are stable`() {
        val done = setOf("breakfast", "morning")
        RecoveryLevel.entries.forEach { level ->
            val plan = PlanBuilder.build(level, done)
            assertTrue(plan.items.first { it.id == "breakfast" }.done)
            // "morning" is absent on a recovery day; that must not throw or leak in.
            plan.items.find { it.id == "morning" }?.let { assertTrue(it.done) }
        }
    }

    @Test
    fun `an unknown done id is ignored`() {
        val plan = PlanBuilder.build(RecoveryLevel.NORMAL, setOf("no_such_item"))
        assertTrue(plan.items.none { it.done })
    }

    @Test
    fun `every level explains itself, and the easier ones cite why`() {
        RecoveryLevel.entries.forEach { level ->
            val plan = PlanBuilder.build(level, emptySet())
            assertTrue("$level has no rationale", plan.rationale.isNotBlank())
        }
        assertTrue(PlanBuilder.build(RecoveryLevel.EASY, emptySet()).source!!.contains("2013"))
        assertTrue(PlanBuilder.build(RecoveryLevel.RECOVERY, emptySet()).source!!.contains("2026"))
    }

    @Test
    fun `no plan text carries medical-advice boilerplate`() {
        val banned = listOf("not medical advice", "consult", "diagnos", "medication", "dosing")
        RecoveryLevel.entries.forEach { level ->
            val plan = PlanBuilder.build(level, emptySet())
            val text = (plan.rationale + plan.items.joinToString(" ") { it.title }).lowercase()
            banned.forEach { assertFalse("$level mentions '$it'", text.contains(it)) }
        }
    }
}
