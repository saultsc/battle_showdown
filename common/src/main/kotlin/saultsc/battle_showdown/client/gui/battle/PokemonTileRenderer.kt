package saultsc.battle_showdown.client.gui.battle

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.font
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.drawProfilePokemon
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.getDepletableRedGreen
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.render.renderScaledGuiItemIcon
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * Draws the content of a team preview tile (model, level, name, gender, status, ball, HP bar).
 * The tile background is drawn by the caller.
 */
object PokemonTileRenderer {
    const val TILE_WIDTH = 94
    const val TILE_HEIGHT = 29
    const val SCALE = 0.5F

    private const val BALL_WIDTH = 18
    private const val BALL_HEIGHT = 22
    private const val HP_BAR_MAX_WIDTH = 90

    val tileTexture = cobblemonResource("textures/gui/battle/party_select.png")
    val tileDisabledTexture = cobblemonResource("textures/gui/battle/party_select_disabled.png")

    class Health(val hp: Int, val maxHp: Int) {
        val ratio: Float = if (maxHp > 0) hp / maxHp.toFloat() else 0F
    }

    fun health(pokemon: Pokemon, showdownPokemon: ShowdownPokemon): Health {
        val healthRatioSplits = showdownPokemon.condition.split(" ")[0].split("/")
        return if (healthRatioSplits.size == 1) Health(0, 0)
        else Health(healthRatioSplits[0].toInt(), pokemon.maxHealth)
    }

    fun render(
        context: GuiGraphics,
        x: Float,
        y: Float,
        pokemon: Pokemon,
        state: FloatingState,
        health: Health,
        isFainted: Boolean,
        hpText: String,
        highlightBall: Boolean = false,
        showHeldItem: Boolean = false,
        partialTicks: Float
    ) {
        state.currentAspects = pokemon.aspects
        val matrixStack = context.pose()

        // Status effect
        val status = pokemon.status?.status?.showdownName
        if (health.ratio > 0F && status != null) {
            blitk(
                matrixStack = matrixStack,
                texture = cobblemonResource("textures/gui/interact/party_select_status_$status.png"),
                x = x + 27,
                y = y + 24,
                height = 5,
                width = 37
            )
            drawScaledText(
                context = context,
                text = lang("ui.status.$status").bold(),
                x = x + 32.5,
                y = y + 24.5,
                shadow = true,
                scale = SCALE
            )
        }

        // Poké Ball
        blitk(
            matrixStack = matrixStack,
            texture = cobblemonResource("textures/gui/ball/${pokemon.caughtBall.name.path}.png"),
            x = (x + 85) / SCALE,
            y = (y - 3) / SCALE,
            height = BALL_HEIGHT,
            width = BALL_WIDTH,
            vOffset = if (highlightBall) BALL_HEIGHT else 0,
            textureHeight = BALL_HEIGHT * 2,
            scale = SCALE
        )

        // Pokémon model
        matrixStack.pushPose()
        matrixStack.translate(x + TILE_WIDTH - (25 / 2.0) - 4, y - 1.0, 0.0)
        matrixStack.scale(2.5F, 2.5F, 1F)
        drawProfilePokemon(
            species = pokemon.species.resourceIdentifier,
            matrixStack = matrixStack,
            rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(13F, 35F, 0F)),
            state = state,
            scale = 4.5F,
            partialTicks = partialTicks
        )
        matrixStack.popPose()

        // UI drawn on top of the model
        matrixStack.pushPose()
        matrixStack.translate(0.0, 0.0, 100.0)

        if (showHeldItem) {
            val heldItem = pokemon.heldItem()
            if (!heldItem.isEmpty) {
                renderScaledGuiItemIcon(
                    matrixStack = matrixStack,
                    itemStack = heldItem,
                    x = x + 81.0,
                    y = y + 11.0,
                    scale = 0.5
                )
            }
        }

        val textOpacity = if (isFainted) 0.7F else 1F

        // Level
        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = lang("ui.lv").bold(),
            x = x + 5,
            y = y + 4,
            opacity = textOpacity,
            shadow = true
        )
        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = pokemon.level.toString().text().bold(),
            x = x + 5 + 13,
            y = y + 4,
            opacity = textOpacity,
            shadow = true
        )

        // Name
        val displayText = pokemon.getDisplayName().bold()
        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = displayText,
            x = x + 5,
            y = y + 11,
            opacity = textOpacity,
            shadow = true
        )

        // Gender
        val gender = pokemon.gender
        if (gender != Gender.GENDERLESS) {
            val displayNameWidth = Minecraft.getInstance().font.width(displayText.font(CobblemonResources.DEFAULT_LARGE))
            val isMale = gender == Gender.MALE
            drawScaledText(
                context = context,
                font = CobblemonResources.DEFAULT_LARGE,
                text = (if (isMale) "♂" else "♀").text().bold(),
                x = x + 6 + displayNameWidth,
                y = y + 11,
                colour = if (isMale) 0x32CBFF else 0xFC5454,
                opacity = textOpacity,
                shadow = true
            )
        }

        // HP bar
        val (red, green) = getDepletableRedGreen(health.ratio)
        blitk(
            matrixStack = matrixStack,
            texture = CobblemonResources.WHITE,
            x = x + 1,
            y = y + 22,
            width = (health.ratio * HP_BAR_MAX_WIDTH).toInt(),
            height = 1,
            red = red * 0.8F,
            green = green * 0.8F,
            blue = 0.27F
        )

        drawScaledText(
            context = context,
            text = hpText.text(),
            x = x + 14,
            y = y + 24.5,
            scale = SCALE,
            centered = true
        )

        matrixStack.popPose()
    }
}
