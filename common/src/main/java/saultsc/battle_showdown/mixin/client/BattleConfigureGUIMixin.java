package saultsc.battle_showdown.mixin.client;

import com.cobblemon.mod.common.client.battle.ClientBattleChallenge;
import com.cobblemon.mod.common.client.gui.interact.battleRequest.BattleConfigureGUI;
import com.cobblemon.mod.common.net.messages.client.PlayerInteractOptionsPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import saultsc.battle_showdown.battle.PreviewFormats;
import saultsc.battle_showdown.client.ChallengePageNavigation;
import saultsc.battle_showdown.client.ChallengeScreenHooks;
import saultsc.battle_showdown.client.PreviewVariantHolder;

import java.util.List;

/**
 * Gives the Singles and Doubles pages of Cobblemon's challenge screen a second "preview" page.
 */
@Mixin(value = BattleConfigureGUI.class, remap = false)
public abstract class BattleConfigureGUIMixin implements PreviewVariantHolder {
    @Shadow
    private int currentPage;

    @Shadow
    @Final
    @Nullable
    private ClientBattleChallenge activeRequest;

    @Shadow
    private static List<? extends PlayerInteractOptionsPacket.Options> options;

    @Unique
    private boolean battle_showdown$previewVariant;

    @Unique
    private boolean battle_showdown$enterVariantAfterMove;

    @Override
    public boolean isBattleShowdownPreviewVariant() {
        // When answering a challenge the variant is whatever the sender chose.
        if (this.activeRequest != null) {
            return PreviewFormats.INSTANCE.of(this.activeRequest.getBattleFormat()) != null;
        }
        return this.battle_showdown$previewVariant;
    }

    @Inject(method = "setCurrentPage", at = @At("HEAD"), cancellable = true)
    private void battle_showdown$beforePageChange(int value, CallbackInfo ci) {
        ChallengePageNavigation.PageChange change = ChallengePageNavigation.INSTANCE.onPageChange(
            this.currentPage,
            value,
            options.size(),
            this.battle_showdown$currentPageSupportsPreview(),
            this.battle_showdown$previewVariant,
            ChallengeScreenHooks.INSTANCE.isMouseOnLeftHalf()
        );

        this.battle_showdown$previewVariant = change.getPreviewVariant();
        this.battle_showdown$enterVariantAfterMove = change.getEnterVariantAfterMove();
        if (change.getCancelMove()) {
            ci.cancel();
        }
    }

    @Inject(method = "setCurrentPage", at = @At("RETURN"))
    private void battle_showdown$afterPageChange(int value, CallbackInfo ci) {
        if (this.battle_showdown$enterVariantAfterMove) {
            this.battle_showdown$enterVariantAfterMove = false;
            this.battle_showdown$previewVariant = this.battle_showdown$currentPageSupportsPreview();
        }
    }

    @Unique
    private boolean battle_showdown$currentPageSupportsPreview() {
        if (this.currentPage < 0 || this.currentPage >= options.size()) {
            return false;
        }
        return ChallengeScreenHooks.INSTANCE.previewFormatOf(options.get(this.currentPage)) != null;
    }
}
