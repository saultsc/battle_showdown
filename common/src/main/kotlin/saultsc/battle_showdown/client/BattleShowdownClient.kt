package saultsc.battle_showdown.client

import net.minecraft.client.Minecraft
import saultsc.battle_showdown.client.gui.battle.BattlePreviewScreen
import saultsc.battle_showdown.network.packets.s2c.BattlePreviewPacket
import saultsc.battle_showdown.network.packets.s2c.BattleTimerUpdatePacket

/**
 * Client side packet handlers. Only ever loaded on the physical client.
 */
object BattleShowdownClient {
    fun handleBattlePreview(packet: BattlePreviewPacket) {
        Minecraft.getInstance().setScreen(
            BattlePreviewScreen(
                battleId = packet.battleId,
                opponentTeam = packet.opponentTeam,
                opponentName = packet.opponentName,
                playerTeam = packet.playerTeam,
                playerName = packet.playerName
            )
        )
    }

    fun handleTimerUpdate(packet: BattleTimerUpdatePacket) {
        val screen = Minecraft.getInstance().screen as? BattlePreviewScreen ?: return
        screen.updateTimer(packet)
    }
}
