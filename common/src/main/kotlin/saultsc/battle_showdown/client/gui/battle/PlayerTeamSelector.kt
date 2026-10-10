package saultsc.battle_showdown.client.gui.battle

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import saultsc.battle_showdown.battle.PreviewPokemon

/**
 * The player's own team. Clicking a tile picks that Pokémon (or drops it again). Tiles stay in their slot
 * and the picked ones show their position in the pick order.
 *
 * The selection itself lives in the screen ([selectedOrder]) so it survives the selector being rebuilt on resize.
 *
 * @param leadCount how many of the first picked Pokémon are sent out first.
 * @param canEdit false once the selection is locked.
 * @param canPickMore false when the selection is full.
 * @param dimUnselected true when the Pokémon that are not picked will not take part in the battle.
 */
class PlayerTeamSelector(
    private val playerTeam: List<PreviewPokemon>,
    private val getSlotPosition: (Int) -> Pair<Float, Float>,
    private val selectedOrder: () -> List<Int>,
    private val leadCount: Int,
    private val canEdit: () -> Boolean,
    private val canPickMore: () -> Boolean,
    private val dimUnselected: () -> Boolean,
    private val onToggle: (Int) -> Unit
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
        val clickedTile = tiles.find { it.isHovered(mouseX, mouseY) && it.isClickable } ?: return false
        onToggle(clickedTile.index)
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

        /** Position in the pick order, or -1 when not picked. */
        private val pickPosition: Int
            get() = selector.selectedOrder().indexOf(index)

        /** Picked Pokémon can be dropped, the others can be picked while there is room. */
        val isClickable: Boolean
            get() = selector.canEdit() && (pickPosition >= 0 || (!pokemon.isFainted && selector.canPickMore()))

        fun isHovered(mouseX: Double, mouseY: Double) =
            mouseX in x.toDouble()..(x + TILE_WIDTH).toDouble() && mouseY in y.toDouble()..(y + TILE_HEIGHT).toDouble()

        fun render(context: GuiGraphics, mouseX: Double, mouseY: Double, deltaTicks: Float) {
            val pickPosition = pickPosition
            val isPicked = pickPosition >= 0
            val isDisabled = pokemon.isFainted || (!isPicked && selector.dimUnselected())
            val isHighlighted = isPicked || (isClickable && isHovered(mouseX, mouseY))

            blitk(
                matrixStack = context.pose(),
                texture = if (isDisabled) tileDisabledTexture else PokemonTileRenderer.tileTexture,
                x = x,
                y = y,
                width = TILE_WIDTH,
                height = TILE_HEIGHT,
                vOffset = if (pokemon.isFainted || (!isDisabled && isHighlighted)) 0 else TILE_HEIGHT,
                textureHeight = TILE_HEIGHT * 2
            )

            PokemonTileRenderer.render(
                context = context,
                x = x,
                y = y,
                pokemon = pokemon,
                state = state,
                hpText = "${pokemon.currentHealth}/${pokemon.maxHealth}",
                highlightBall = pickPosition in 0 until selector.leadCount,
                orderBadge = if (isPicked) pickPosition + 1 else null,
                partialTicks = deltaTicks
            )
        }
    }
}
