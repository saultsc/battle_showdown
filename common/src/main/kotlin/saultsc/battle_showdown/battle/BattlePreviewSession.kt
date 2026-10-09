package saultsc.battle_showdown.battle

import com.cobblemon.mod.common.battles.BattleFormat
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.getPlayer
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

    fun sideOf(playerId: UUID): PreviewSide? = sides.firstOrNull { it.playerId == playerId }

    fun involves(playerId: UUID): Boolean = sideOf(playerId) != null
}

/**
 * One player of a [BattlePreviewSession].
 *
 * Players are stored by UUID because the [ServerPlayer] instance changes when a player respawns.
 *
 * @param team the Pokémon shown in the preview (clones when the format adjusts levels).
 * @param partyIds the UUIDs of the real party Pokémon, in the same order as [team].
 */
class PreviewSide(
    val playerId: UUID,
    val playerName: String,
    val team: List<Pokemon>,
    val partyIds: List<UUID>
) {
    var selection: UUID? = null

    /** The current online player, or null if they left. */
    val player: ServerPlayer?
        get() = playerId.getPlayer()

    val hasSelectablePokemon: Boolean
        get() = team.any { !it.isFainted() }

    fun previewTeam(revealHeldItems: Boolean): List<PreviewPokemon> = team.map { PreviewPokemon.of(it, revealHeldItems) }
}
