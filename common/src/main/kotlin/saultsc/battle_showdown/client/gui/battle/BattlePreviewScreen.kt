package saultsc.battle_showdown.client.gui.battle

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import saultsc.battle_showdown.BattleShowdown
import saultsc.battle_showdown.battle.BattlePreviewManager
import saultsc.battle_showdown.network.packets.c2s.PokemonSelectionPacket
import saultsc.battle_showdown.network.packets.s2c.BattleTimerUpdatePacket
import saultsc.battle_showdown.network.packets.s2c.BattleTimerUpdatePacket.TimerPhase
import java.util.UUID

/**
 * Team preview shown to both players before a PvP battle starts.
 * The player's team is on the left and can be clicked to pick a lead, the rival's team is on the right.
 */
class BattlePreviewScreen(
    private val battleId: UUID,
    private val opponentTeam: List<Pair<ShowdownPokemon, Pokemon>>,
    private val opponentName: String,
    private val playerTeam: List<Pair<ShowdownPokemon, Pokemon>>,
    private val playerName: String
) : Screen(Component.translatable("${BattleShowdown.MOD_ID}.ui.battle_preview")) {

    companion object {
        const val SLOT_HORIZONTAL_SPACING = 4F
        const val SLOT_VERTICAL_SPACING = 2F
        const val TEAM_SIDE_MARGIN = 20
        const val BACKGROUND_HEIGHT = 148
        const val PARTY_SIZE = 6
        val underlayTexture = cobblemonResource("textures/gui/battle/selection_underlay.png")
    }

    private lateinit var rivalTeamDisplay: RivalTeamDisplay
    private lateinit var playerTeamSelector: PlayerTeamSelector
    private var backgroundY: Int = 0

    private var selectionTimeRemaining: Int = BattlePreviewManager.SELECTION_TIME_LIMIT
    private var preStartTimeRemaining: Int = BattlePreviewManager.PRE_START_TIME_LIMIT
    private var currentPhase: TimerPhase = TimerPhase.SELECTION
    private var hasSelectedPokemon: Boolean = false

    override fun shouldCloseOnEsc() = false

    override fun isPauseScreen() = false

    override fun init() {
        super.init()
        backgroundY = if (height > 304) (height / 2) - (BACKGROUND_HEIGHT / 2)
        else height - (BACKGROUND_HEIGHT + 78)

        rivalTeamDisplay = RivalTeamDisplay(opponentTeam, ::getRivalSlotPosition)
        rivalTeamDisplay.init()

        playerTeamSelector = PlayerTeamSelector(playerTeam, ::getPlayerSlotPosition, ::onPokemonSelected)
        playerTeamSelector.init()
    }

    private fun onPokemonSelected(selectedIndex: Int) {
        if (currentPhase != TimerPhase.SELECTION || hasSelectedPokemon) return
        hasSelectedPokemon = true
        BattleShowdown.networkManager.sendToServer(PokemonSelectionPacket(battleId, selectedIndex))
    }

    fun updateTimer(timerUpdate: BattleTimerUpdatePacket) {
        if (battleId != timerUpdate.battleId) return

        selectionTimeRemaining = timerUpdate.selectionTimeRemaining
        preStartTimeRemaining = timerUpdate.preStartTimeRemaining
        currentPhase = timerUpdate.phase

        if (currentPhase == TimerPhase.FINISHED) {
            onClose()
        }
    }

    private fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

    private val playerTeamWidth: Float
        get() = (PlayerTeamSelector.PlayerTeamTile.TILE_WIDTH * 2) + SLOT_HORIZONTAL_SPACING

    private val rivalTeamWidth: Float
        get() = (RivalTeamDisplay.TeamPreviewTile.TILE_WIDTH * 2) + SLOT_HORIZONTAL_SPACING

    private val playerTeamStartX: Float
        get() = (width / 2F) - playerTeamWidth - TEAM_SIDE_MARGIN

    private val rivalTeamStartX: Float
        get() = (width / 2F) + TEAM_SIDE_MARGIN

    private fun getPlayerSlotPosition(index: Int): Pair<Float, Float> = getSlotPosition(
        startX = playerTeamStartX,
        index = index,
        tileWidth = PlayerTeamSelector.PlayerTeamTile.TILE_WIDTH,
        tileHeight = PlayerTeamSelector.PlayerTeamTile.TILE_HEIGHT
    )

    private fun getRivalSlotPosition(index: Int): Pair<Float, Float> = getSlotPosition(
        startX = rivalTeamStartX,
        index = index,
        tileWidth = RivalTeamDisplay.TeamPreviewTile.TILE_WIDTH,
        tileHeight = RivalTeamDisplay.TeamPreviewTile.TILE_HEIGHT
    )

    private fun getSlotPosition(startX: Float, index: Int, tileWidth: Int, tileHeight: Int): Pair<Float, Float> {
        val startY = backgroundY + 34F
        val row = index / 2
        val column = index % 2
        val slotX = startX + column * (SLOT_HORIZONTAL_SPACING + tileWidth)
        val slotY = startY + row * (SLOT_VERTICAL_SPACING + tileHeight)
        return slotX to slotY
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val matrixStack = context.pose()

        blitk(
            matrixStack = matrixStack,
            texture = underlayTexture,
            x = 0,
            y = backgroundY,
            width = width,
            height = BACKGROUND_HEIGHT
        )

        // Player team (left)
        renderTeamTitle(context, Component.translatable("${BattleShowdown.MOD_ID}.ui.party", playerName), playerTeamStartX, playerTeamWidth)
        for (i in 0 until PARTY_SIZE) {
            val (playerX, playerY) = getPlayerSlotPosition(i)
            renderEmptySlot(context, playerX, playerY, PlayerTeamSelector.PlayerTeamTile.TILE_WIDTH, PlayerTeamSelector.PlayerTeamTile.TILE_HEIGHT)
        }

        // Rival team (right)
        renderTeamTitle(context, Component.translatable("${BattleShowdown.MOD_ID}.ui.party", opponentName), rivalTeamStartX, rivalTeamWidth)
        for (i in 0 until PARTY_SIZE) {
            val (rivalX, rivalY) = getRivalSlotPosition(i)
            renderEmptySlot(context, rivalX, rivalY, RivalTeamDisplay.TeamPreviewTile.TILE_WIDTH, RivalTeamDisplay.TeamPreviewTile.TILE_HEIGHT)
        }

        playerTeamSelector.render(context, mouseX, mouseY, delta)
        rivalTeamDisplay.render(context, delta)

        renderTimer(context)
    }

    private fun renderTeamTitle(context: GuiGraphics, title: MutableComponent, teamStartX: Float, teamWidth: Float) {
        drawScaledText(
            context = context,
            text = title,
            x = teamStartX + (teamWidth / 2) - (font.width(title) / 2F),
            y = backgroundY + 17F,
            shadow = true
        )
    }

    private fun renderEmptySlot(context: GuiGraphics, x: Float, y: Float, tileWidth: Int, tileHeight: Int) {
        blitk(
            matrixStack = context.pose(),
            texture = PlayerTeamSelector.PlayerTeamTile.tileDisabledTexture,
            x = x,
            y = y,
            width = tileWidth,
            height = tileHeight,
            vOffset = tileHeight,
            textureHeight = tileHeight * 2
        )
    }

    private fun renderTimer(context: GuiGraphics) {
        val instructionY = backgroundY + 138F
        val timerDisplayY = instructionY + 25F

        when (currentPhase) {
            TimerPhase.SELECTION -> {
                val instructionText = if (hasSelectedPokemon) {
                    Component.translatable("${BattleShowdown.MOD_ID}.ui.waiting_for_rival").withStyle(ChatFormatting.YELLOW)
                } else {
                    Component.translatable("${BattleShowdown.MOD_ID}.ui.select_pokemon").withStyle(ChatFormatting.WHITE)
                }
                renderCenteredText(context, instructionText, instructionY)

                val timeLabel = Component.translatable("${BattleShowdown.MOD_ID}.ui.time_remaining").withStyle(ChatFormatting.GRAY)
                renderCenteredText(context, timeLabel, timerDisplayY)

                val timerText = Component.literal(formatTime(selectionTimeRemaining))
                    .withStyle(if (selectionTimeRemaining <= 10) ChatFormatting.RED else ChatFormatting.WHITE)
                renderCenteredText(context, timerText, timerDisplayY + 12F)
            }

            TimerPhase.PRE_START -> {
                val startLabel = Component.translatable("${BattleShowdown.MOD_ID}.ui.starting_in").withStyle(ChatFormatting.GREEN)
                renderCenteredText(context, startLabel, timerDisplayY)

                val timerText = Component.literal(formatTime(preStartTimeRemaining)).withStyle(ChatFormatting.GREEN)
                renderCenteredText(context, timerText, timerDisplayY + 12F)
            }

            // The screen closes itself as soon as the session finishes.
            TimerPhase.FINISHED -> Unit
        }
    }

    private fun renderCenteredText(context: GuiGraphics, text: MutableComponent, y: Float) {
        drawScaledText(
            context = context,
            text = text,
            x = (width / 2F) - (font.width(text) / 2F),
            y = y,
            shadow = true
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (currentPhase == TimerPhase.SELECTION && !hasSelectedPokemon && playerTeamSelector.mouseClicked(mouseX, mouseY)) {
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }
}
