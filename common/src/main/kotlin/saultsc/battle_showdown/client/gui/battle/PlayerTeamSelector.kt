package saultsc.battle_showdown.client.gui.battle

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import saultsc.battle_showdown.battle.PreviewPokemon

/**
 * The player's own team. Clicking a non fainted tile selects it as the lead Pokémon.
 *
 * The selection lives in the screen ([selectedIndex]) so it survives the selector being rebuilt on resize.
 */
class PlayerTeamSelector(
    private val playerTeam: List<PreviewPokemon>,
    private val getSlotPosition: (Int) -> Pair<Float, Float>,
    private val selectedIndex: () -> Int?,
    private val canSelect: () -> Boolean,
    private val onPokemonSelected: (Int) -> Unit
) {
    private val tiles = mutableListOf<PlayerTeamTile>()

    fun init() {
        tiles.clear()
        playerTeam.forEachIndexed { index, pokemon ->
            val (slotX, slotY) = getSlotPosition(index)
            tiles.add(PlayerTeamTile(this, slotX, slotY, pokemon, index))
        }
    }

    fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        tiles.forEach { it.render(context, mouseX.toDouble(), mouseY.toDouble(), delta) }
    }

    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!canSelect()) return false
        val clickedTile = tiles.find { it.isHovered(mouseX, mouseY) && !it.pokemon.isFainted } ?: return false
        onPokemonSelected(clickedTile.index)
        Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.GUI_CLICK, 1.0F))
        return true
    }

    class PlayerTeamTile(
        private val selector: PlayerTeamSelector,
        private val x: Float,
        private val y: Float,
        val pokemon: PreviewPokemon,
        val index: Int
    ) {
        companion object {
            const val TILE_WIDTH = PokemonTileRenderer.TILE_WIDTH
            const val TILE_HEIGHT = PokemonTileRenderer.TILE_HEIGHT
            val tileDisabledTexture = PokemonTileRenderer.tileDisabledTexture
        }

        private val state = FloatingState()

        fun isHovered(mouseX: Double, mouseY: Double) =
            mouseX in x.toDouble()..(x + TILE_WIDTH).toDouble() && mouseY in y.toDouble()..(y + TILE_HEIGHT).toDouble()

        fun render(context: GuiGraphics, mouseX: Double, mouseY: Double, deltaTicks: Float) {
            val isSelected = selector.selectedIndex() == index
            val isHighlighted = selector.canSelect() && !pokemon.isFainted && isHovered(mouseX, mouseY)

            blitk(
                matrixStack = context.pose(),
                texture = if (!pokemon.isFainted && !isSelected) PokemonTileRenderer.tileTexture else tileDisabledTexture,
                x = x,
                y = y,
                width = TILE_WIDTH,
                height = TILE_HEIGHT,
                vOffset = if (pokemon.isFainted || isHighlighted) 0 else TILE_HEIGHT,
                textureHeight = TILE_HEIGHT * 2
            )

            PokemonTileRenderer.render(
                context = context,
                x = x,
                y = y,
                pokemon = pokemon,
                state = state,
                hpText = "${pokemon.currentHealth}/${pokemon.maxHealth}",
                highlightBall = isSelected,
                partialTicks = deltaTicks
            )
        }
    }
}
