package saultsc.battle_showdown.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TeamSelectionTest {
    private val fullTeam = List(6) { true }

    @Test
    fun `singles lets the player order the whole team but only requires the lead`() {
        assertEquals(6, TeamSelection.maxSelections(PreviewFormat.SINGLES, fullTeam))
        assertEquals(1, TeamSelection.requiredSelections(PreviewFormat.SINGLES, fullTeam))
    }

    @Test
    fun `doubles picks exactly four of six`() {
        assertEquals(4, TeamSelection.maxSelections(PreviewFormat.DOUBLES, fullTeam))
        assertEquals(4, TeamSelection.requiredSelections(PreviewFormat.DOUBLES, fullTeam))
    }

    @Test
    fun `doubles picks every usable pokemon when there are fewer than four`() {
        val team = listOf(true, false, true, true)
        assertEquals(3, TeamSelection.maxSelections(PreviewFormat.DOUBLES, team))
        assertEquals(3, TeamSelection.requiredSelections(PreviewFormat.DOUBLES, team))
    }

    @Test
    fun `selection must be distinct, in range and not fainted`() {
        val team = listOf(true, true, false, true, true, true)
        assertTrue(TeamSelection.isValid(listOf(5, 0, 3), team, PreviewFormat.SINGLES))
        assertFalse(TeamSelection.isValid(listOf(0, 0), team, PreviewFormat.SINGLES))
        assertFalse(TeamSelection.isValid(listOf(6), team, PreviewFormat.SINGLES))
        assertFalse(TeamSelection.isValid(listOf(-1), team, PreviewFormat.SINGLES))
        assertFalse(TeamSelection.isValid(listOf(2), team, PreviewFormat.SINGLES))
    }

    @Test
    fun `doubles rejects more than four picks`() {
        assertTrue(TeamSelection.isValid(listOf(0, 1, 2, 3), fullTeam, PreviewFormat.DOUBLES))
        assertFalse(TeamSelection.isValid(listOf(0, 1, 2, 3, 4), fullTeam, PreviewFormat.DOUBLES))
    }

    @Test
    fun `confirming needs the required amount`() {
        assertFalse(TeamSelection.canConfirm(emptyList(), fullTeam, PreviewFormat.SINGLES))
        assertTrue(TeamSelection.canConfirm(listOf(5), fullTeam, PreviewFormat.SINGLES))
        assertFalse(TeamSelection.canConfirm(listOf(0, 1, 2), fullTeam, PreviewFormat.DOUBLES))
        assertTrue(TeamSelection.canConfirm(listOf(3, 2, 1, 0), fullTeam, PreviewFormat.DOUBLES))
    }

    @Test
    fun `singles order puts the picks first and keeps the rest of the team`() {
        assertEquals(listOf(5, 2, 0, 1, 3, 4), TeamSelection.resolveOrder(listOf(5, 2), fullTeam, PreviewFormat.SINGLES))
    }

    @Test
    fun `singles keeps fainted pokemon in the team behind the picks`() {
        val team = listOf(false, true, true)
        assertEquals(listOf(2, 0, 1), TeamSelection.resolveOrder(listOf(2), team, PreviewFormat.SINGLES))
    }

    @Test
    fun `doubles order brings only the four picks`() {
        assertEquals(listOf(5, 4, 1, 0), TeamSelection.resolveOrder(listOf(5, 4, 1, 0), fullTeam, PreviewFormat.DOUBLES))
    }

    @Test
    fun `a partial doubles selection is completed with the team order`() {
        assertEquals(listOf(4, 0, 1, 2), TeamSelection.resolveOrder(listOf(4), fullTeam, PreviewFormat.DOUBLES))
    }

    @Test
    fun `auto completing never brings fainted pokemon in doubles`() {
        val team = listOf(false, true, true, false, true, true)
        assertEquals(listOf(1, 2, 4, 5), TeamSelection.resolveOrder(emptyList(), team, PreviewFormat.DOUBLES))
    }

    @Test
    fun `an empty selection keeps the original team order`() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5), TeamSelection.resolveOrder(emptyList(), fullTeam, PreviewFormat.SINGLES))
    }
}
