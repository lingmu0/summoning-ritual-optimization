package net.xuwu.summoning_ritual_optimization.mixin.client;

import net.xuwu.summoning_ritual_optimization.compat.jei.JeiMobOutputHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.Polarice3.Goety.compat.jei.ModRitualCategory", remap = false)
public abstract class GoetyRitualJeiCategoryMixin {
    @Inject(method = "setRecipe", at = @At("TAIL"), remap = false)
    private void summoningRitualOptimization$showSummonedMob(Object builder, Object recipe, Object focuses, CallbackInfo ci) {
        JeiMobOutputHelper.addGoety1201Output(builder, recipe);
    }
}
