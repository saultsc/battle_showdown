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
        PlatformEvents.SERVER_PLAYER_LOGOUT.subscribe { onPlayerLogout(it.player) }
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

        val sender = challenge.sender
        val receiver = challenge.receiver

        val busyPlayer = listOf(sender, receiver).firstOrNull(::isInPreview)
        if (busyPlayer != null) {
            val message = Component.translatable("${BattleShowdown.MOD_ID}.preview.busy", busyPlayer.name).red()
            sender.sendSystemMessage(message)
            receiver.sendSystemMessage(message)
            return true
        }

        val session = BattlePreviewSession(
            battleId = UUID.randomUUID(),
            battleFormat = challenge.battleFormat,
            sides = listOf(sender, receiver).map { player ->
                val party = Cobblemon.storage.getParty(player).toList()
                PreviewSide(
                    player = player,
                    team = BattleUtils.getPreviewTeam(party, challenge.battleFormat),
                    partyIds = party.map { it.uuid }
                )
            }
        )
        sessions[session.battleId] = session

        val (senderSide, receiverSide) = session.sides
        sendPreview(session, senderSide, receiverSide)
        sendPreview(session, receiverSide, senderSide)
        return true
    }

    fun handlePokemonSelection(battleId: UUID, player: ServerPlayer, selectedIndex: Int) {
        val session = sessions[battleId] ?: return
        if (session.phase != TimerPhase.SELECTION) return

        val side = session.sideOf(player) ?: return
        if (side.selection != null) return

        val (showdownPokemon, _) = side.team.getOrNull(selectedIndex) ?: return
        if (BattleUtils.isFainted(showdownPokemon)) return

        side.selection = side.partyIds[selectedIndex]

        if (session.allSelected) {
            enterPreStart(session)
        }
    }

    fun isInPreview(player: ServerPlayer): Boolean = sessions.values.any { it.involves(player) }

    private fun sendPreview(session: BattlePreviewSession, side: PreviewSide, opponent: PreviewSide) {
        BattleShowdown.networkManager.sendToPlayer(
            side.player,
            BattlePreviewPacket(
                battleId = session.battleId,
                playerTeam = side.team,
                playerName = side.player.name.string,
                opponentTeam = opponent.team,
                opponentName = opponent.player.name.string
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
     * If only one player picked a lead, the other one forfeits. If nobody did, the battle is cancelled.
     */
    private fun handleSelectionTimeout(session: BattlePreviewSession) {
        val notSelected = session.sides.filter { it.selection == null }
        val message = if (notSelected.size == 1) {
            Component.translatable("${BattleShowdown.MOD_ID}.preview.timeout.forfeit", notSelected.first().player.name)
        } else {
            Component.translatable("${BattleShowdown.MOD_ID}.preview.timeout.cancelled")
        }
        notifyAll(session, message.red())
        finish(session)
    }

    private fun startBattle(session: BattlePreviewSession) {
        finish(session)

        val (side1, side2) = session.sides
        BattleBuilder.pvp1v1(
            side1.player,
            side2.player,
            side1.selection,
            side2.selection,
            session.battleFormat
        ).ifErrored { error ->
            session.sides.forEach { side -> error.sendTo(side.player) { it.red() } }
        }
    }

    private fun onPlayerLogout(player: ServerPlayer) {
        sessions.values.filter { it.involves(player) }.forEach { session ->
            finish(session)
            val message = Component.translatable("${BattleShowdown.MOD_ID}.preview.cancelled.disconnect", player.name).red()
            session.sides.filter { it.player.uuid != player.uuid }.forEach { it.player.sendSystemMessage(message) }
        }
    }

    private fun finish(session: BattlePreviewSession) {
        session.phase = TimerPhase.FINISHED
        sessions.remove(session.battleId)
        sendTimerUpdate(session)
    }

    private fun sendTimerUpdate(session: BattlePreviewSession) {
        val packet = BattleTimerUpdatePacket(
            battleId = session.battleId,
            selectionTimeRemaining = session.selectionTimeRemaining,
            preStartTimeRemaining = session.preStartTimeRemaining,
            phase = session.phase
        )
        session.sides
            .filterNot { it.player.hasDisconnected() }
            .forEach { BattleShowdown.networkManager.sendToPlayer(it.player, packet) }
    }

    private fun notifyAll(session: BattlePreviewSession, message: Component) {
        session.sides.forEach { it.player.sendSystemMessage(message) }
    }
}
