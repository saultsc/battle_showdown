package saultsc.battle_showdown.battle

import kotlin.math.min

/**
 * Rules of the ordered team selection. Works on team indices only, so the same logic runs on client and server.
 *
 * `selectable` has one entry per team member: true when that Pokémon can be picked (not fainted).
 */
object TeamSelection {
    /** The most Pokémon a player can pick. */
    fun maxSelections(format: PreviewFormat, selectable: List<Boolean>): Int {
        val selectableCount = selectable.count { it }
        return if (format.bringsOnlySelected) min(format.maxBrought, selectableCount) else selectableCount
    }

    /** The fewest Pokémon a player must pick before confirming. */
    fun requiredSelections(format: PreviewFormat, selectable: List<Boolean>): Int =
        if (format.bringsOnlySelected) maxSelections(format, selectable)
        else min(format.leadCount, selectable.count { it })

    /** True when [selected] is a legal (possibly incomplete) selection. */
    fun isValid(selected: List<Int>, selectable: List<Boolean>, format: PreviewFormat): Boolean =
        selected.size == selected.toSet().size &&
            selected.all { selectable.getOrNull(it) == true } &&
            selected.size <= maxSelections(format, selectable)

    fun canConfirm(selected: List<Int>, selectable: List<Boolean>, format: PreviewFormat): Boolean =
        isValid(selected, selectable, format) && selected.size >= requiredSelections(format, selectable)

    /**
     * The final team order: the selected Pokémon first, then the remaining ones in team order.
     * Used both when the player confirms and to auto complete a selection when the time runs out.
     *
     * When the format only brings the selected Pokémon, the result is cut to the allowed amount and never
     * includes Pokémon that cannot be picked. Otherwise the whole team is returned.
     */
    fun resolveOrder(selected: List<Int>, selectable: List<Boolean>, format: PreviewFormat): List<Int> {
        val picked = selected.distinct().filter { selectable.getOrNull(it) == true }
        val remaining = selectable.indices.filter { it !in picked }

        return if (format.bringsOnlySelected) {
            (picked + remaining.filter { selectable[it] }).take(maxSelections(format, selectable))
        } else {
            picked + remaining
        }
    }
}
