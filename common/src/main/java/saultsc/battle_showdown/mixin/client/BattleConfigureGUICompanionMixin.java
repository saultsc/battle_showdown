package saultsc.battle_showdown.mixin.client;

import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.client.gui.interact.battleRequest.BattleConfigureGUI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import saultsc.battle_showdown.client.ChallengeScreenHooks;

/**
 * Sends the challenge with the preview format when it is requested from a preview page.
 */
@Mixin(value = BattleConfigureGUI.Companion.class, remap = false)
public abstract class BattleConfigureGUICompanionMixin {
    @ModifyVariable(method = "sendBattleRequest", at = @At("HEAD"), argsOnly = true)
    private BattleFormat battle_showdown$applyPreviewVariant(BattleFormat battleFormat) {
        return ChallengeScreenHooks.INSTANCE.applyPreviewVariant(battleFormat);
    }
}
