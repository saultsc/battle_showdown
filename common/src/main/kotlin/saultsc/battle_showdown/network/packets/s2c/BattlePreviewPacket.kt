package saultsc.battle_showdown.network.packets.s2c

import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import saultsc.battle_showdown.BattleShowdown
import saultsc.battle_showdown.client.BattleShowdownClient
import saultsc.battle_showdown.network.ClientboundPacket
import java.util.UUID

/**
 * Opens the team preview screen on the client with both teams.
 */
data class BattlePreviewPacket(
    val battleId: UUID,
    val playerTeam: List<Pair<ShowdownPokemon, Pokemon>>,
    val playerName: String,
    val opponentTeam: List<Pair<ShowdownPokemon, Pokemon>>,
    val opponentName: String
) : ClientboundPacket {

    override fun type(): CustomPacketPayload.Type<BattlePreviewPacket> = TYPE

    override fun handle() = BattleShowdownClient.handleBattlePreview(this)

    companion object {
        val TYPE = CustomPacketPayload.Type<BattlePreviewPacket>(BattleShowdown.id("battle_preview"))

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, BattlePreviewPacket> = StreamCodec.of(::write, ::read)

        private fun write(buf: RegistryFriendlyByteBuf, packet: BattlePreviewPacket) {
            buf.writeUUID(packet.battleId)
            writeTeam(buf, packet.playerTeam)
            buf.writeUtf(packet.playerName)
            writeTeam(buf, packet.opponentTeam)
            buf.writeUtf(packet.opponentName)
        }

        private fun read(buf: RegistryFriendlyByteBuf): BattlePreviewPacket = BattlePreviewPacket(
            battleId = buf.readUUID(),
            playerTeam = readTeam(buf),
            playerName = buf.readUtf(),
            opponentTeam = readTeam(buf),
            opponentName = buf.readUtf()
        )

        private fun writeTeam(buf: RegistryFriendlyByteBuf, team: List<Pair<ShowdownPokemon, Pokemon>>) {
            buf.writeVarInt(team.size)
            team.forEach { (showdownPokemon, pokemon) ->
                Pokemon.S2C_CODEC.encode(buf, pokemon)
                buf.writeUtf(showdownPokemon.condition)
                buf.writeUtf(showdownPokemon.pokeball)
            }
        }

        private fun readTeam(buf: RegistryFriendlyByteBuf): List<Pair<ShowdownPokemon, Pokemon>> {
            val size = buf.readVarInt()
            return List(size) {
                val pokemon = Pokemon.S2C_CODEC.decode(buf)
                val showdownPokemon = ShowdownPokemon().apply {
                    condition = buf.readUtf()
                    pokeball = buf.readUtf()
                }
                showdownPokemon to pokemon
            }
        }
    }
}
