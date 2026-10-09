package saultsc.battle_showdown.mixin;

import com.cobblemon.mod.common.battles.ChallengeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import saultsc.battle_showdown.battle.BattlePreviewManager;

/**
 * Intercepts accepted PvP challenges so both players go through the team preview
 * before Cobblemon starts the battle.
 */
@Mixin(value = ChallengeManager.class, remap = false)
public abstract class ChallengeManagerMixin {
    @Inject(
        method = "onAccept(Lcom/cobblemon/mod/common/battles/ChallengeManager$BattleChallenge;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void battle_showdown$onAccept(ChallengeManager.BattleChallenge challenge, CallbackInfo ci) {
        if (BattlePreviewManager.INSTANCE.onChallengeAccepted(challenge)) {
            ci.cancel();
        }
    }
}
