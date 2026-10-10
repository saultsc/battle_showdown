package saultsc.battle_showdown.battle

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.storage.party.PartyStore
import com.cobblemon.mod.common.api.text.red
import com.cobblemon.mod.common.battles.BattleBuilder
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
     * Only challenges sent with one of the [PreviewFormat]s go through the preview, every other battle is left to Cobblemon.
     *
     * @return true if the team preview takes over the challenge and Cobblemon must not start the battle itself.
     */
    fun onChallengeAccepted(challenge: ChallengeManager.BattleChallenge): Boolean {
        if (challenge !is ChallengeManager.SinglesBattleChallenge) return false
        val previewFormat = PreviewFormats.of(challenge.battleFormat) ?: return false
        val battleFormat = PreviewFormats.strip(challenge.battleFormat)

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
                team = BattleUtils.getPreviewTeam(party, battleFormat),
                partyIds = party.map { it.uuid }
            )
        }

        // Not enough Pokémon to pick the leads: let Cobblemon start the battle so it reports its own error right away.
        // The marker rule has to go first, Showdown does not know it.
        if (sides.any { side -> side.selectable.count { it } < previewFormat.leadCount }) {
            challenge.battleFormat.ruleSet = battleFormat.ruleSet
            return false
        }

        val session = BattlePreviewSession(UUID.randomUUID(), previewFormat, battleFormat, sides)
        sessions[session.battleId] = session

        val (senderSide, receiverSide) = sides
        sendPreview(session, senderSide, receiverSide)
        sendPreview(session, receiverSide, senderSide)
        return true
    }

    /**
     * Stores the player's current selection. Once [confirmed] the side is locked, and the battle countdown
     * starts when both players have confirmed.
     */
    fun handleTeamSelection(battleId: UUID, player: ServerPlayer, selectedIndices: List<Int>, confirmed: Boolean) {
        val session = sessions[battleId] ?: return
        if (session.phase != TimerPhase.SELECTION) return

        val side = session.sideOf(player.uuid) ?: return
        if (side.confirmed) return
        if (!TeamSelection.isValid(selectedIndices, side.selectable, session.previewFormat)) return

        side.selection = selectedIndices

        if (confirmed && TeamSelection.canConfirm(selectedIndices, side.selectable, session.previewFormat)) {
            side.confirmed = true
            if (session.allConfirmed) {
                enterPreStart(session)
            }
        }
    }

    fun isInPreview(player: ServerPlayer): Boolean = sessions.values.any { it.involves(player.uuid) }

    private fun sendPreview(session: BattlePreviewSession, side: PreviewSide, opponent: PreviewSide) {
        val player = side.player ?: return
        BattleShowdown.networkManager.sendToPlayer(
            player,
            BattlePreviewPacket(
                battleId = session.battleId,
                format = session.previewFormat,
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
     * Out of time: whoever did not confirm is locked with what they had picked so far.
     * The missing picks are filled in with the team order when the battle starts.
     */
    private fun handleSelectionTimeout(session: BattlePreviewSession) {
        session.sides.forEach { it.confirmed = true }
        enterPreStart(session)
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

        val battleParties: Map<UUID, PartyStore> = mapOf(
            player1.uuid to battleParty(session, side1, player1),
            player2.uuid to battleParty(session, side2, player2)
        )

        BattleBuilder.pvp1v1(
            player1 = player1,
            player2 = player2,
            battleFormat = session.battleFormat,
            partyAccessor = { player -> battleParties.getValue(player.uuid) }
        ).ifErrored { error ->
            listOf(player1, player2).forEach { player -> error.sendTo(player) { it.red() } }
        }
    }

    /**
     * The Pokémon that enter the battle for [side], in battle order. They are looked up in the real party
     * so a Pokémon that left the party during the preview is simply skipped.
     */
    private fun battleParty(session: BattlePreviewSession, side: PreviewSide, player: ServerPlayer): PartyStore {
        val party = Cobblemon.storage.getParty(player).toList()
        val ordered = TeamSelection.resolveOrder(side.selection, side.selectable, session.previewFormat)
            .map { side.partyIds[it] }
            .mapNotNull { id -> party.firstOrNull { it.uuid == id } }
        return OrderedPartyView(player.uuid, ordered)
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
