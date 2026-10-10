package saultsc.battle_showdown.network.packets.s2c

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import saultsc.battle_showdown.BattleShowdown
import saultsc.battle_showdown.battle.PreviewFormat
import saultsc.battle_showdown.battle.PreviewPokemon
import saultsc.battle_showdown.client.BattleShowdownClient
import saultsc.battle_showdown.network.ClientboundPacket
import java.util.UUID

/**
 * Opens the team preview screen on the client with both teams.
 */
data class BattlePreviewPacket(
    val battleId: UUID,
    val format: PreviewFormat,
    val playerTeam: List<PreviewPokemon>,
    val playerName: String,
    val opponentTeam: List<PreviewPokemon>,
    val opponentName: String
) : ClientboundPacket {

    override fun type(): CustomPacketPayload.Type<BattlePreviewPacket> = TYPE

    override fun handle() = BattleShowdownClient.handleBattlePreview(this)

    companion object {
        val TYPE = CustomPacketPayload.Type<BattlePreviewPacket>(BattleShowdown.id("battle_preview"))

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, BattlePreviewPacket> = StreamCodec.of(::write, ::read)

        private fun write(buf: RegistryFriendlyByteBuf, packet: BattlePreviewPacket) {
            buf.writeUUID(packet.battleId)
            buf.writeEnum(packet.format)
            writeTeam(buf, packet.playerTeam)
            buf.writeUtf(packet.playerName)
            writeTeam(buf, packet.opponentTeam)
            buf.writeUtf(packet.opponentName)
        }

        private fun read(buf: RegistryFriendlyByteBuf): BattlePreviewPacket = BattlePreviewPacket(
            battleId = buf.readUUID(),
            format = buf.readEnum(PreviewFormat::class.java),
            playerTeam = readTeam(buf),
            playerName = buf.readUtf(),
            opponentTeam = readTeam(buf),
            opponentName = buf.readUtf()
        )

        private fun writeTeam(buf: RegistryFriendlyByteBuf, team: List<PreviewPokemon>) {
            buf.writeVarInt(team.size)
            team.forEach { PreviewPokemon.STREAM_CODEC.encode(buf, it) }
        }

        private fun readTeam(buf: RegistryFriendlyByteBuf): List<PreviewPokemon> =
            List(buf.readVarInt()) { PreviewPokemon.STREAM_CODEC.decode(buf) }
    }
}
