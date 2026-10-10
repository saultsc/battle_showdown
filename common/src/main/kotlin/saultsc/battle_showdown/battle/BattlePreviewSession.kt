package saultsc.battle_showdown.battle

import com.cobblemon.mod.common.battles.BattleFormat
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.getPlayer
import net.minecraft.server.level.ServerPlayer
import saultsc.battle_showdown.network.packets.s2c.BattleTimerUpdatePacket.TimerPhase
import java.util.UUID

/**
 * A pending PvP battle where both players are looking at the team preview.
 *
 * @param battleFormat the format the battle will be started with (without the preview marker rule).
 */
class BattlePreviewSession(
    val battleId: UUID,
    val previewFormat: PreviewFormat,
    val battleFormat: BattleFormat,
    val sides: List<PreviewSide>
) {
    var phase: TimerPhase = TimerPhase.SELECTION
    var selectionTimeRemaining: Int = previewFormat.selectionSeconds
    var preStartTimeRemaining: Int = BattlePreviewManager.PRE_START_TIME_LIMIT
    var ticksUntilNextSecond: Int = BattlePreviewManager.TICKS_PER_SECOND

    val allConfirmed: Boolean
        get() = sides.all { it.confirmed }

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
    /** Team indices in the order the player picked them. May be incomplete until the battle starts. */
    var selection: List<Int> = emptyList()
    var confirmed: Boolean = false

    /** One entry per team member: true when it can be picked. */
    val selectable: List<Boolean> = team.map { !it.isFainted() }

    /** The current online player, or null if they left. */
    val player: ServerPlayer?
        get() = playerId.getPlayer()

    fun previewTeam(revealHeldItems: Boolean): List<PreviewPokemon> = team.map { PreviewPokemon.of(it, revealHeldItems) }
}
