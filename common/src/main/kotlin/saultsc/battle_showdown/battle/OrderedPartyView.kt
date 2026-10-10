package saultsc.battle_showdown.battle

import com.cobblemon.mod.common.api.storage.party.PartyStore
import com.cobblemon.mod.common.pokemon.Pokemon
import java.util.UUID

/**
 * A read only view over some party Pokémon in a chosen order.
 *
 * Cobblemon's battle builder creates the battle team by iterating the party it is given, so handing it this view
 * decides both which Pokémon enter the battle and in which order, without moving them out of the real party.
 */
class OrderedPartyView(playerId: UUID, private val ordered: List<Pokemon>) : PartyStore(playerId) {
    override fun iterator(): Iterator<Pokemon> = ordered.iterator()
}
