package saultsc.battle_showdown.client.gui.battle

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import saultsc.battle_showdown.util.BattleUtils

/**
 * The player's own team. Clicking a non fainted tile selects it as the lead Pokémon.
 */
class PlayerTeamSelector(
    private val playerTeam: List<Pair<ShowdownPokemon, Pokemon>>,
    private val getSlotPosition: (Int) -> Pair<Float, Float>,
    private val onPokemonSelected: (Int) -> Unit = {}
) {
    private val tiles = mutableListOf<PlayerTeamTile>()
    var selectedPokemon: Pokemon? = null
        private set

    fun init() {
        tiles.clear()
        playerTeam.forEachIndexed { index, (showdownPokemon, pokemon) ->
            val (slotX, slotY) = getSlotPosition(index)
            tiles.add(PlayerTeamTile(this, slotX, slotY, pokemon, showdownPokemon, index))
        }
    }

    fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        tiles.forEach { it.render(context, mouseX.toDouble(), mouseY.toDouble(), delta) }
    }

    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        val clickedTile = tiles.find { it.isHovered(mouseX, mouseY) && !it.isFainted } ?: return false
        selectedPokemon = clickedTile.pokemon
        onPokemonSelected(clickedTile.index)
        Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.GUI_CLICK, 1.0F))
        return true
    }

    class PlayerTeamTile(
        private val selector: PlayerTeamSelector,
        private val x: Float,
        private val y: Float,
        val pokemon: Pokemon,
        private val showdownPokemon: ShowdownPokemon,
        val index: Int
    ) {
        companion object {
            const val TILE_WIDTH = PokemonTileRenderer.TILE_WIDTH
            const val TILE_HEIGHT = PokemonTileRenderer.TILE_HEIGHT
            val tileDisabledTexture = PokemonTileRenderer.tileDisabledTexture
        }

        val isFainted = BattleUtils.isFainted(showdownPokemon)
        private val state = FloatingState()

        fun isHovered(mouseX: Double, mouseY: Double) =
            mouseX in x.toDouble()..(x + TILE_WIDTH).toDouble() && mouseY in y.toDouble()..(y + TILE_HEIGHT).toDouble()

        fun render(context: GuiGraphics, mouseX: Double, mouseY: Double, deltaTicks: Float) {
            val health = PokemonTileRenderer.health(pokemon, showdownPokemon)
            val isSelected = selector.selectedPokemon == pokemon

            blitk(
                matrixStack = context.pose(),
                texture = if (!isFainted && !isSelected) PokemonTileRenderer.tileTexture else tileDisabledTexture,
                x = x,
                y = y,
                width = TILE_WIDTH,
                height = TILE_HEIGHT,
                vOffset = if (isFainted || (!isSelected && isHovered(mouseX, mouseY))) 0 else TILE_HEIGHT,
                textureHeight = TILE_HEIGHT * 2
            )

            PokemonTileRenderer.render(
                context = context,
                x = x,
                y = y,
                pokemon = pokemon,
                state = state,
                health = health,
                isFainted = isFainted,
                hpText = "${health.hp}/${health.maxHp}",
                highlightBall = isSelected,
                showHeldItem = true,
                partialTicks = deltaTicks
            )
        }
    }
}
