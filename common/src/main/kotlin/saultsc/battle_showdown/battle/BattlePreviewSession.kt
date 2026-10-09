package saultsc.battle_showdown.battle

import com.cobblemon.mod.common.battles.BattleFormat
import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.server.level.ServerPlayer
import saultsc.battle_showdown.network.packets.s2c.BattleTimerUpdatePacket.TimerPhase
import java.util.UUID

/**
 * A pending PvP battle where both players are looking at the team preview.
 */
class BattlePreviewSession(
    val battleId: UUID,
    val battleFormat: BattleFormat,
    val sides: List<PreviewSide>
) {
    var phase: TimerPhase = TimerPhase.SELECTION
    var selectionTimeRemaining: Int = BattlePreviewManager.SELECTION_TIME_LIMIT
    var preStartTimeRemaining: Int = BattlePreviewManager.PRE_START_TIME_LIMIT
    var ticksUntilNextSecond: Int = BattlePreviewManager.TICKS_PER_SECOND

    val allSelected: Boolean
        get() = sides.all { it.selection != null }

    fun sideOf(player: ServerPlayer): PreviewSide? = sides.firstOrNull { it.player.uuid == player.uuid }

    fun involves(player: ServerPlayer): Boolean = sideOf(player) != null
}

/**
 * One player of a [BattlePreviewSession].
 *
 * @param team the team as sent to the clients.
 * @param partyIds the UUIDs of the real party Pokémon, in the same order as [team].
 */
class PreviewSide(
    val player: ServerPlayer,
    val team: List<Pair<ShowdownPokemon, Pokemon>>,
    val partyIds: List<UUID>
) {
    var selection: UUID? = null
}
