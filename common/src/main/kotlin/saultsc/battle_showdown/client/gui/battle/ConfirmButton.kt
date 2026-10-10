package saultsc.battle_showdown.client.gui.battle

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.MutableComponent

/**
 * Confirm button drawn with the same texture Cobblemon uses for its battle challenge button.
 * The texture holds the normal state on top and the hovered state below it.
 */
class ConfirmButton(
    x: Int,
    y: Int,
    private val text: MutableComponent,
    onPress: OnPress
) : Button(x, y, WIDTH, HEIGHT, text, onPress, DEFAULT_NARRATION) {

    companion object {
        const val WIDTH = 69
        const val HEIGHT = 19
        private const val INACTIVE_OPACITY = 0.5F
        private val buttonTexture = cobblemonResource("textures/gui/interact/request/button_request_battle.png")
    }

    override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val opacity = if (active) 1F else INACTIVE_OPACITY

        blitk(
            matrixStack = context.pose(),
            texture = buttonTexture,
            x = x,
            y = y,
            width = WIDTH,
            height = HEIGHT,
            vOffset = if (active && isHovered) HEIGHT else 0,
            textureHeight = HEIGHT * 2,
            alpha = opacity
        )

        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = text.copy().bold(),
            x = x + WIDTH / 2,
            y = y + 5,
            opacity = opacity,
            centered = true,
            shadow = true
        )
    }

    override fun playDownSound(soundManager: SoundManager) {
        soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.GUI_CLICK, 1.0F))
    }
}
