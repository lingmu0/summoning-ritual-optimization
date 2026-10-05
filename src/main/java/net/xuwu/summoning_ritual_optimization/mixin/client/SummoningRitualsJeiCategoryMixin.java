package net.xuwu.summoning_ritual_optimization.mixin.client;

import net.xuwu.summoning_ritual_optimization.compat.jei.JeiMobOutputHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.almostreliable.summoningrituals.compat.viewer.jei.AltarCategoryJEI", remap = false)
public abstract class SummoningRitualsJeiCategoryMixin {
    @Inject(method = "setRecipe", at = @At("TAIL"), remap = false)
    private void summoningRitualOptimization$showMobEggs(@Coerce Object builder, @Coerce Object recipe, @Coerce Object focuses, CallbackInfo ci) {
        JeiMobOutputHelper.addSummoningRituals1201Outputs(builder, recipe);
    }
}
