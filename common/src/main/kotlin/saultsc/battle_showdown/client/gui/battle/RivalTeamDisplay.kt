package saultsc.battle_showdown.client.gui.battle

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import net.minecraft.client.gui.GuiGraphics
import saultsc.battle_showdown.battle.PreviewPokemon

/**
 * Read only view of the rival's team. HP is shown as a percentage.
 */
class RivalTeamDisplay(
    private val opponentTeam: List<PreviewPokemon>,
    private val getSlotPosition: (Int) -> Pair<Float, Float>
) {
    private val tiles = mutableListOf<TeamPreviewTile>()

    fun init() {
        tiles.clear()
        opponentTeam.forEachIndexed { index, pokemon ->
            val (slotX, slotY) = getSlotPosition(index)
            tiles.add(TeamPreviewTile(slotX, slotY, pokemon))
        }
    }

    fun render(context: GuiGraphics, delta: Float) {
        tiles.forEach { it.render(context, delta) }
    }

    class TeamPreviewTile(
        private val x: Float,
        private val y: Float,
        private val pokemon: PreviewPokemon
    ) {
        companion object {
            const val TILE_WIDTH = PokemonTileRenderer.TILE_WIDTH
            const val TILE_HEIGHT = PokemonTileRenderer.TILE_HEIGHT
            val tileDisabledTexture = PokemonTileRenderer.tileDisabledTexture
        }

        private val state = FloatingState()

        fun render(context: GuiGraphics, deltaTicks: Float) {
            blitk(
                matrixStack = context.pose(),
                texture = tileDisabledTexture,
                x = x,
                y = y,
                width = TILE_WIDTH,
                height = TILE_HEIGHT,
                vOffset = if (pokemon.isFainted) 0 else TILE_HEIGHT,
                textureHeight = TILE_HEIGHT * 2
            )

            PokemonTileRenderer.render(
                context = context,
                x = x,
                y = y,
                pokemon = pokemon,
                state = state,
                hpText = "${(pokemon.healthRatio * 100).toInt()}%",
                partialTicks = deltaTicks
            )
        }
    }
}
