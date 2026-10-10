package saultsc.battle_showdown.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import saultsc.battle_showdown.network.packets.c2s.TeamSelectionPacket
import saultsc.battle_showdown.network.packets.s2c.BattlePreviewPacket
import saultsc.battle_showdown.network.packets.s2c.BattleTimerUpdatePacket

/** A packet sent from the server to the client. [handle] runs on the client main thread. */
interface ClientboundPacket : CustomPacketPayload {
    fun handle()
}

/** A packet sent from the client to the server. [handle] runs on the server main thread. */
interface ServerboundPacket : CustomPacketPayload {
    fun handle(player: ServerPlayer)
}

/**
 * Receives every packet of the mod so each loader can register them with its own networking API.
 */
interface PacketRegistrar {
    fun <T : ClientboundPacket> clientbound(type: CustomPacketPayload.Type<T>, codec: StreamCodec<RegistryFriendlyByteBuf, T>)

    fun <T : ServerboundPacket> serverbound(type: CustomPacketPayload.Type<T>, codec: StreamCodec<RegistryFriendlyByteBuf, T>)
}

object BattleShowdownNetwork {
    fun register(registrar: PacketRegistrar) {
        registrar.clientbound(BattlePreviewPacket.TYPE, BattlePreviewPacket.CODEC)
        registrar.clientbound(BattleTimerUpdatePacket.TYPE, BattleTimerUpdatePacket.CODEC)
        registrar.serverbound(TeamSelectionPacket.TYPE, TeamSelectionPacket.CODEC)
    }
}
