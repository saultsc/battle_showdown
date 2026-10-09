package saultsc.battle_showdown.neoforge.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import saultsc.battle_showdown.BattleShowdown
import saultsc.battle_showdown.network.BattleShowdownNetwork
import saultsc.battle_showdown.network.ClientboundPacket
import saultsc.battle_showdown.network.NetworkManager
import saultsc.battle_showdown.network.PacketRegistrar
import saultsc.battle_showdown.network.ServerboundPacket

object NeoForgeNetworkManager : NetworkManager {
    private const val PROTOCOL_VERSION = "1"

    fun registerPayloads(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar(BattleShowdown.MOD_ID).versioned(PROTOCOL_VERSION)

        BattleShowdownNetwork.register(object : PacketRegistrar {
            override fun <T : ClientboundPacket> clientbound(type: CustomPacketPayload.Type<T>, codec: StreamCodec<RegistryFriendlyByteBuf, T>) {
                registrar.playToClient(type, codec) { packet, _ -> packet.handle() }
            }

            override fun <T : ServerboundPacket> serverbound(type: CustomPacketPayload.Type<T>, codec: StreamCodec<RegistryFriendlyByteBuf, T>) {
                registrar.playToServer(type, codec) { packet, context -> packet.handle(context.player() as ServerPlayer) }
            }
        })
    }

    override fun sendToPlayer(player: ServerPlayer, packet: CustomPacketPayload) = PacketDistributor.sendToPlayer(player, packet)

    override fun sendToServer(packet: CustomPacketPayload) = PacketDistributor.sendToServer(packet)
}
