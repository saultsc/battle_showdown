package saultsc.battle_showdown.network

import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

/**
 * Platform specific packet sending. Implemented by each loader module.
 */
interface NetworkManager {
    fun sendToPlayer(player: ServerPlayer, packet: CustomPacketPayload)

    fun sendToServer(packet: CustomPacketPayload)
}
