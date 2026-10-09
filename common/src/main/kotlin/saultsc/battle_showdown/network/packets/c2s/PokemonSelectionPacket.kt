package saultsc.battle_showdown.network.packets.c2s

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import saultsc.battle_showdown.BattleShowdown
import saultsc.battle_showdown.battle.BattlePreviewManager
import saultsc.battle_showdown.network.ServerboundPacket
import java.util.UUID

/**
 * Sent when the player picks their lead Pokémon in the team preview.
 * [selectedPokemonIndex] is the index inside the team the server sent in [saultsc.battle_showdown.network.packets.s2c.BattlePreviewPacket].
 */
data class PokemonSelectionPacket(
    val battleId: UUID,
    val selectedPokemonIndex: Int
) : ServerboundPacket {

    override fun type(): CustomPacketPayload.Type<PokemonSelectionPacket> = TYPE

    override fun handle(player: ServerPlayer) =
        BattlePreviewManager.handlePokemonSelection(battleId, player, selectedPokemonIndex)

    companion object {
        val TYPE = CustomPacketPayload.Type<PokemonSelectionPacket>(BattleShowdown.id("pokemon_selection"))

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, PokemonSelectionPacket> = StreamCodec.of(::write, ::read)

        private fun write(buf: RegistryFriendlyByteBuf, packet: PokemonSelectionPacket) {
            buf.writeUUID(packet.battleId)
            buf.writeVarInt(packet.selectedPokemonIndex)
        }

        private fun read(buf: RegistryFriendlyByteBuf): PokemonSelectionPacket = PokemonSelectionPacket(
            battleId = buf.readUUID(),
            selectedPokemonIndex = buf.readVarInt()
        )
    }
}
