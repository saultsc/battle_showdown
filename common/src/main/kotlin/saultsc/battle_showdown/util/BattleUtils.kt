package saultsc.battle_showdown.util

import com.cobblemon.mod.common.battles.BattleFormat
import com.cobblemon.mod.common.pokemon.Pokemon

object BattleUtils {
    /**
     * Builds the team shown in the preview, mirroring what Cobblemon's battle builder will use:
     * when the format adjusts levels the Pokémon are cloned, levelled and healed, otherwise they are shown as they are.
     * The party itself is never modified.
     */
    fun getPreviewTeam(party: List<Pokemon>, battleFormat: BattleFormat): List<Pokemon> {
        val adjustLevel = battleFormat.adjustLevel
        if (adjustLevel <= 0) return party

        return party.map { original ->
            original.clone().apply {
                level = adjustLevel
                heal()
            }
        }
    }
}
