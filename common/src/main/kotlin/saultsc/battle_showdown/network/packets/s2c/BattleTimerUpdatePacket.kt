package saultsc.battle_showdown.network.packets.s2c

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import saultsc.battle_showdown.BattleShowdown
import saultsc.battle_showdown.client.BattleShowdownClient
import saultsc.battle_showdown.network.ClientboundPacket
import java.util.UUID

/**
 * Syncs the team preview timers. A [TimerPhase.FINISHED] update closes the preview screen.
 */
data class BattleTimerUpdatePacket(
    val battleId: UUID,
    /** Seconds left to pick a lead Pokémon. */
    val selectionTimeRemaining: Int,
    /** Seconds left before the battle starts. */
    val preStartTimeRemaining: Int,
    val phase: TimerPhase
) : ClientboundPacket {

    enum class TimerPhase {
        SELECTION,
        PRE_START,
        FINISHED
    }

    override fun type(): CustomPacketPayload.Type<BattleTimerUpdatePacket> = TYPE

    override fun handle() = BattleShowdownClient.handleTimerUpdate(this)

    companion object {
        val TYPE = CustomPacketPayload.Type<BattleTimerUpdatePacket>(BattleShowdown.id("battle_timer_update"))

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, BattleTimerUpdatePacket> = StreamCodec.of(::write, ::read)

        private fun write(buf: RegistryFriendlyByteBuf, packet: BattleTimerUpdatePacket) {
            buf.writeUUID(packet.battleId)
            buf.writeVarInt(packet.selectionTimeRemaining)
            buf.writeVarInt(packet.preStartTimeRemaining)
            buf.writeEnum(packet.phase)
        }

        private fun read(buf: RegistryFriendlyByteBuf): BattleTimerUpdatePacket = BattleTimerUpdatePacket(
            battleId = buf.readUUID(),
            selectionTimeRemaining = buf.readVarInt(),
            preStartTimeRemaining = buf.readVarInt(),
            phase = buf.readEnum(TimerPhase::class.java)
        )
    }
}
