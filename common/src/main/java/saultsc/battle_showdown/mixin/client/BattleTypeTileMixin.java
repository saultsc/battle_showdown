package saultsc.battle_showdown.mixin.client;

import com.cobblemon.mod.common.client.gui.interact.battleRequest.BattleConfigureGUI;
import com.cobblemon.mod.common.net.messages.client.PlayerInteractOptionsPacket;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import saultsc.battle_showdown.client.ChallengeScreenHooks;

/**
 * Shows "Singles Preview" / "Doubles Preview" as the battle type name on the preview pages.
 */
@Mixin(value = BattleConfigureGUI.BattleTypeTile.class, remap = false)
public abstract class BattleTypeTileMixin {
    @Shadow
    @Final
    private PlayerInteractOptionsPacket.Options option;

    @Inject(method = "getSubTitle", at = @At("RETURN"), cancellable = true)
    private void battle_showdown$previewSubTitle(CallbackInfoReturnable<MutableComponent> cir) {
        MutableComponent previewSubTitle = ChallengeScreenHooks.INSTANCE.previewSubTitle(this.option);
        if (previewSubTitle != null) {
            cir.setReturnValue(previewSubTitle);
        }
    }
}
