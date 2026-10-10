package saultsc.battle_showdown.battle

import com.cobblemon.mod.common.battles.BattleFormat

/**
 * The battle formats that go through the team preview.
 *
 * @param battleTypeName the Cobblemon battle type this format is played as.
 * @param leadCount how many of the first selected Pokémon are sent out at the start.
 * @param maxBrought how many Pokémon take part in the battle at most.
 * @param bringsOnlySelected true when only the selected Pokémon enter the battle (bring 6, pick 4),
 * false when the whole team enters and the selection only decides the order.
 * @param selectionSeconds time the players have to confirm their selection.
 */
enum class PreviewFormat(
    val battleTypeName: String,
    val leadCount: Int,
    val maxBrought: Int,
    val bringsOnlySelected: Boolean,
    val selectionSeconds: Int
) {
    SINGLES(battleTypeName = "singles", leadCount = 1, maxBrought = 6, bringsOnlySelected = false, selectionSeconds = 60),
    DOUBLES(battleTypeName = "doubles", leadCount = 2, maxBrought = 4, bringsOnlySelected = true, selectionSeconds = 60);

    val translationKey: String = "battle_showdown.format.${name.lowercase()}_preview"
}

/**
 * Preview formats travel inside Cobblemon's [BattleFormat] as a marker rule, the same way Cobblemon carries its own
 * non Showdown rules. The marker must be stripped before the battle starts because Showdown rejects unknown rules.
 */
object PreviewFormats {
    const val MARKER_RULE = "Battle Showdown Preview"

    fun of(format: BattleFormat): PreviewFormat? {
        if (MARKER_RULE !in format.ruleSet) return null
        return ofBattleType(format.battleType.name)
    }

    fun ofBattleType(battleTypeName: String): PreviewFormat? =
        PreviewFormat.entries.firstOrNull { it.battleTypeName.equals(battleTypeName, ignoreCase = true) }

    fun mark(format: BattleFormat): BattleFormat = format.copy(ruleSet = format.ruleSet + MARKER_RULE)

    fun strip(format: BattleFormat): BattleFormat = format.copy(ruleSet = format.ruleSet - MARKER_RULE)
}
