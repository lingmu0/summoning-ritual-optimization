package net.xuwu.summoning_ritual_optimization.mixin.client;

import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.ingredients.IIngredientType;
import net.xuwu.summoning_ritual_optimization.compat.jei.SummoningRitualsMobOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.almostreliable.summoningrituals.compat.viewer.jei.AltarCategoryJEI", remap = false)
public abstract class SummoningRitualsJeiCategoryMixin {
    @Redirect(method = "lambda$setRecipe$3", at = @At(value = "INVOKE",
            target = "Lmezz/jei/api/gui/builder/IRecipeSlotBuilder;addIngredient(Lmezz/jei/api/ingredients/IIngredientType;Ljava/lang/Object;)Lmezz/jei/api/gui/builder/IIngredientAcceptor;"),
            remap = false, require = 1)
    private IIngredientAcceptor<?> summoningRitualOptimization$useSpawnEgg(
            IRecipeSlotBuilder slot, IIngredientType<?> type, Object ingredient) {
        return SummoningRitualsMobOutput.add(slot, ingredient, this);
    }
}
