package saultsc.battle_showdown.util

import com.cobblemon.mod.common.battles.BattleFormat
import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.mod.common.pokemon.Pokemon

object BattleUtils {
    /**
     * Builds the team shown in the preview, mirroring what Cobblemon's battle builder will use:
     * when the format adjusts levels the Pokémon are cloned, levelled and healed, otherwise they are shown as they are.
     */
    fun getPreviewTeam(party: List<Pokemon>, battleFormat: BattleFormat): List<Pair<ShowdownPokemon, Pokemon>> {
        val adjustLevel = battleFormat.adjustLevel
        return party.map { original ->
            val pokemon = if (adjustLevel > 0) {
                original.clone().apply {
                    level = adjustLevel
                    heal()
                }
            } else {
                original
            }

            val condition = "${pokemon.currentHealth}/${pokemon.maxHealth}" + if (pokemon.isFainted()) " fnt" else ""

            ShowdownPokemon().apply {
                this.condition = condition
                this.pokeball = pokemon.caughtBall.name.toString()
            } to pokemon
        }
    }

    fun isFainted(showdownPokemon: ShowdownPokemon) = "fnt" in showdownPokemon.condition
}
