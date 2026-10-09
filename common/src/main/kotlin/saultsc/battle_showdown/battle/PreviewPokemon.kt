package saultsc.battle_showdown.battle

import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

/**
 * What a client is allowed to see of a Pokémon in the team preview.
 *
 * Only visual data is sent, so the rival never receives moves, IVs, EVs, nature or ability.
 */
data class PreviewPokemon(
    val species: ResourceLocation,
    val aspects: Set<String>,
    val displayName: Component,
    val level: Int,
    val gender: Gender,
    val caughtBall: ResourceLocation,
    val heldItem: ItemStack,
    val status: String?,
    val currentHealth: Int,
    val maxHealth: Int,
    val isFainted: Boolean
) {
    val healthRatio: Float
        get() = if (maxHealth > 0) currentHealth / maxHealth.toFloat() else 0F

    companion object {
        /**
         * @param revealHeldItem false for the rival's team, held items stay hidden like in Showdown.
         */
        fun of(pokemon: Pokemon, revealHeldItem: Boolean) = PreviewPokemon(
            species = pokemon.species.resourceIdentifier,
            aspects = pokemon.aspects.toSet(),
            displayName = pokemon.getDisplayName(),
            level = pokemon.level,
            gender = pokemon.gender,
            caughtBall = pokemon.caughtBall.name,
            heldItem = if (revealHeldItem) pokemon.heldItem().copy() else ItemStack.EMPTY,
            status = pokemon.status?.status?.showdownName,
            currentHealth = pokemon.currentHealth,
            maxHealth = pokemon.maxHealth,
            isFainted = pokemon.isFainted()
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PreviewPokemon> = StreamCodec.of(::write, ::read)

        private fun write(buf: RegistryFriendlyByteBuf, pokemon: PreviewPokemon) {
            buf.writeResourceLocation(pokemon.species)
            buf.writeVarInt(pokemon.aspects.size)
            pokemon.aspects.forEach(buf::writeUtf)
            ComponentSerialization.STREAM_CODEC.encode(buf, pokemon.displayName)
            buf.writeVarInt(pokemon.level)
            buf.writeEnum(pokemon.gender)
            buf.writeResourceLocation(pokemon.caughtBall)
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, pokemon.heldItem)
            buf.writeBoolean(pokemon.status != null)
            pokemon.status?.let(buf::writeUtf)
            buf.writeVarInt(pokemon.currentHealth)
            buf.writeVarInt(pokemon.maxHealth)
            buf.writeBoolean(pokemon.isFainted)
        }

        private fun read(buf: RegistryFriendlyByteBuf): PreviewPokemon = PreviewPokemon(
            species = buf.readResourceLocation(),
            aspects = List(buf.readVarInt()) { buf.readUtf() }.toSet(),
            displayName = ComponentSerialization.STREAM_CODEC.decode(buf),
            level = buf.readVarInt(),
            gender = buf.readEnum(Gender::class.java),
            caughtBall = buf.readResourceLocation(),
            heldItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
            status = if (buf.readBoolean()) buf.readUtf() else null,
            currentHealth = buf.readVarInt(),
            maxHealth = buf.readVarInt(),
            isFainted = buf.readBoolean()
        )
    }
}
