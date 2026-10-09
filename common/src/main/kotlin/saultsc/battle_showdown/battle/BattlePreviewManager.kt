package saultsc.battle_showdown.battle

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.text.red
import com.cobblemon.mod.common.battles.BattleBuilder
import com.cobblemon.mod.common.battles.BattleTypes
import com.cobblemon.mod.common.battles.ChallengeManager
import com.cobblemon.mod.common.platform.events.PlatformEvents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import saultsc.battle_showdown.BattleShowdown
import saultsc.battle_showdown.network.packets.s2c.BattlePreviewPacket
import saultsc.battle_showdown.network.packets.s2c.BattleTimerUpdatePacket
import saultsc.battle_showdown.network.packets.s2c.BattleTimerUpdatePacket.TimerPhase
import saultsc.battle_showdown.util.BattleUtils
import java.util.UUID

/**
 * Runs the team preview between an accepted challenge and the start of the battle.
 *
 * Everything here runs on the server thread: timers are driven by the server tick.
 */
object BattlePreviewManager {
    const val SELECTION_TIME_LIMIT = 30
    const val PRE_START_TIME_LIMIT = 5
    const val TICKS_PER_SECOND = 20

    private val sessions = mutableMapOf<UUID, BattlePreviewSession>()

    fun register() {
        PlatformEvents.SERVER_TICK_POST.subscribe { tick() }
        PlatformEvents.SERVER_PLAYER_LOGOUT.subscribe { cancelSessionsOf(it.player, "${BattleShowdown.MOD_ID}.preview.cancelled.disconnect") }
        PlatformEvents.PLAYER_DEATH.subscribe { cancelSessionsOf(it.player, "${BattleShowdown.MOD_ID}.preview.cancelled.death") }
        PlatformEvents.SERVER_STOPPING.subscribe { sessions.clear() }
    }

    /**
     * Called when a challenge is accepted.
     *
     * @return true if the team preview takes over the challenge and Cobblemon must not start the battle itself.
     */
    fun onChallengeAccepted(challenge: ChallengeManager.BattleChallenge): Boolean {
        if (challenge !is ChallengeManager.SinglesBattleChallenge) return false
        if (!challenge.battleFormat.battleType.name.equals(BattleTypes.SINGLES.name, ignoreCase = true)) return false

        val players = listOf(challenge.sender, challenge.receiver)

        val busyPlayer = players.firstOrNull(::isInPreview)
        if (busyPlayer != null) {
            val message = Component.translatable("${BattleShowdown.MOD_ID}.preview.busy", busyPlayer.name).red()
            players.forEach { it.sendSystemMessage(message) }
            return true
        }

        val sides = players.map { player ->
            val party = Cobblemon.storage.getParty(player).toList()
            PreviewSide(
                playerId = player.uuid,
                playerName = player.name.string,
                team = BattleUtils.getPreviewTeam(party, challenge.battleFormat),
                partyIds = party.map { it.uuid }
            )
        }

        // Nothing to pick from: let Cobblemon start the battle so it reports its own error right away.
        if (sides.any { !it.hasSelectablePokemon }) return false

        val session = BattlePreviewSession(UUID.randomUUID(), challenge.battleFormat, sides)
        sessions[session.battleId] = session

        val (senderSide, receiverSide) = sides
        sendPreview(session, senderSide, receiverSide)
        sendPreview(session, receiverSide, senderSide)
        return true
    }

    fun handlePokemonSelection(battleId: UUID, player: ServerPlayer, selectedIndex: Int) {
        val session = sessions[battleId] ?: return
        if (session.phase != TimerPhase.SELECTION) return

        val side = session.sideOf(player.uuid) ?: return
        if (side.selection != null) return

        val pokemon = side.team.getOrNull(selectedIndex) ?: return
        if (pokemon.isFainted()) return

        side.selection = side.partyIds[selectedIndex]

        if (session.allSelected) {
            enterPreStart(session)
        }
    }

    fun isInPreview(player: ServerPlayer): Boolean = sessions.values.any { it.involves(player.uuid) }

    private fun sendPreview(session: BattlePreviewSession, side: PreviewSide, opponent: PreviewSide) {
        val player = side.player ?: return
        BattleShowdown.networkManager.sendToPlayer(
            player,
            BattlePreviewPacket(
                battleId = session.battleId,
                playerTeam = side.previewTeam(revealHeldItems = true),
                playerName = side.playerName,
                opponentTeam = opponent.previewTeam(revealHeldItems = false),
                opponentName = opponent.playerName
            )
        )
    }

    private fun tick() {
        if (sessions.isEmpty()) return
        sessions.values.toList().forEach(::tickSession)
    }

    private fun tickSession(session: BattlePreviewSession) {
        if (--session.ticksUntilNextSecond > 0) return
        session.ticksUntilNextSecond = TICKS_PER_SECOND

        when (session.phase) {
            TimerPhase.SELECTION -> {
                session.selectionTimeRemaining--
                if (session.selectionTimeRemaining <= 0) {
                    handleSelectionTimeout(session)
                } else {
                    sendTimerUpdate(session)
                }
            }

            TimerPhase.PRE_START -> {
                session.preStartTimeRemaining--
                if (session.preStartTimeRemaining <= 0) {
                    startBattle(session)
                } else {
                    sendTimerUpdate(session)
                }
            }

            TimerPhase.FINISHED -> Unit
        }
    }

    private fun enterPreStart(session: BattlePreviewSession) {
        session.phase = TimerPhase.PRE_START
        session.preStartTimeRemaining = PRE_START_TIME_LIMIT
        session.ticksUntilNextSecond = TICKS_PER_SECOND
        sendTimerUpdate(session)
    }

    /**
     * The battle is cancelled when someone did not pick a lead in time.
     */
    private fun handleSelectionTimeout(session: BattlePreviewSession) {
        val notSelected = session.sides.filter { it.selection == null }
        val message = if (notSelected.size == 1) {
            Component.translatable("${BattleShowdown.MOD_ID}.preview.timeout.not_chosen", notSelected.first().playerName)
        } else {
            Component.translatable("${BattleShowdown.MOD_ID}.preview.timeout.cancelled")
        }
        notifyAll(session, message.red())
        finish(session)
    }

    private fun startBattle(session: BattlePreviewSession) {
        finish(session)

        val (side1, side2) = session.sides
        val player1 = side1.player
        val player2 = side2.player
        if (player1 == null || player2 == null) {
            val missing = if (player1 == null) side1 else side2
            notifyAll(session, Component.translatable("${BattleShowdown.MOD_ID}.preview.cancelled.disconnect", missing.playerName).red())
            return
        }

        BattleBuilder.pvp1v1(
            player1,
            player2,
            side1.selection,
            side2.selection,
            session.battleFormat
        ).ifErrored { error ->
            listOf(player1, player2).forEach { player -> error.sendTo(player) { it.red() } }
        }
    }

    private fun cancelSessionsOf(player: ServerPlayer, messageKey: String) {
        sessions.values.filter { it.involves(player.uuid) }.forEach { session ->
            finish(session, except = player.uuid)
            val message = Component.translatable(messageKey, player.name).red()
            session.sides
                .filter { it.playerId != player.uuid }
                .forEach { it.player?.sendSystemMessage(message) }
        }
    }

    /**
     * Ends the session and closes the preview screens.
     *
     * @param except a player who must not receive the closing update (e.g. disconnecting).
     */
    private fun finish(session: BattlePreviewSession, except: UUID? = null) {
        session.phase = TimerPhase.FINISHED
        sessions.remove(session.battleId)
        sendTimerUpdate(session, except)
    }

    private fun sendTimerUpdate(session: BattlePreviewSession, except: UUID? = null) {
        val packet = BattleTimerUpdatePacket(
            battleId = session.battleId,
            selectionTimeRemaining = session.selectionTimeRemaining,
            preStartTimeRemaining = session.preStartTimeRemaining,
            phase = session.phase
        )
        session.sides
            .filter { it.playerId != except }
            .mapNotNull { it.player }
            .forEach { BattleShowdown.networkManager.sendToPlayer(it, packet) }
    }

    private fun notifyAll(session: BattlePreviewSession, message: Component) {
        session.sides.mapNotNull { it.player }.forEach { it.sendSystemMessage(message) }
    }
}
