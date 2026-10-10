package saultsc.battle_showdown.client

import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.battles.BattleFormat
import com.cobblemon.mod.common.net.messages.client.PlayerInteractOptionsPacket
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import saultsc.battle_showdown.battle.PreviewFormat
import saultsc.battle_showdown.battle.PreviewFormats

/**
 * Implemented by Cobblemon's battle challenge screen through a mixin.
 */
interface PreviewVariantHolder {
    /** True while the screen is showing (or answering) one of the preview formats instead of the plain battle type. */
    fun isBattleShowdownPreviewVariant(): Boolean
}

/**
 * Glue between the challenge screen mixins and the preview formats.
 * Adds the "Singles Preview" and "Doubles Preview" entries to Cobblemon's battle challenge screen.
 */
object ChallengeScreenHooks {
    fun previewFormatOf(option: PlayerInteractOptionsPacket.Options?): PreviewFormat? = when (option) {
        PlayerInteractOptionsPacket.Options.SINGLE_BATTLE -> PreviewFormat.SINGLES
        PlayerInteractOptionsPacket.Options.DOUBLE_BATTLE -> PreviewFormat.DOUBLES
        else -> null
    }

    /** True when the mouse is on the left half of the screen, where the "previous" button is. */
    fun isMouseOnLeftHalf(): Boolean {
        val minecraft = Minecraft.getInstance()
        return minecraft.mouseHandler.xpos() < minecraft.window.screenWidth / 2.0
    }

    /** The name shown instead of the battle type when the preview variant is active, or null to keep Cobblemon's. */
    fun previewSubTitle(option: PlayerInteractOptionsPacket.Options): MutableComponent? {
        if (!isPreviewVariantActive()) return null
        val format = previewFormatOf(option) ?: return null
        return Component.translatable(format.translationKey).bold()
    }

    /** Marks the format of an outgoing challenge when it is being sent from a preview page. */
    fun applyPreviewVariant(format: BattleFormat): BattleFormat {
        if (!isPreviewVariantActive()) return format
        if (PreviewFormats.ofBattleType(format.battleType.name) == null) return format
        return PreviewFormats.mark(format)
    }

    private fun isPreviewVariantActive(): Boolean =
        (Minecraft.getInstance().screen as? PreviewVariantHolder)?.isBattleShowdownPreviewVariant() == true
}
