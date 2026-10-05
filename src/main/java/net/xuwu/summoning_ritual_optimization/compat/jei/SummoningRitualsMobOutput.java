package net.xuwu.summoning_ritual_optimization.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public final class SummoningRitualsMobOutput {
    private SummoningRitualsMobOutput() {
    }

    public static IIngredientAcceptor<?> add(IRecipeSlotBuilder slot, Object mob, Object category) {
        try {
            EntityType<?> entityType;
            int count;
            if (category == null) {
                Object info = invoke(mob, "getEntityInfo");
                entityType = (EntityType<?>) invoke(invoke(info, "entity"), "value");
                count = ((Number) invoke(info, "count")).intValue();
            } else {
                entityType = (EntityType<?>) invoke(mob, "getEntityType");
                count = ((Number) invoke(mob, "getCount")).intValue();
            }
            SpawnEggItem egg = JeiMobOutputHelper.findSpawnEgg(entityType);
            ItemStack stack = egg == null ? Items.STRUCTURE_VOID.getDefaultInstance() : new ItemStack(egg);
            Component tooltip = Component.translatable(entityType.getDescriptionId())
                    .append(Component.literal(" \u00D7 " + count));
            IIngredientRenderer<Object> entityRenderer = getEntityRenderer(mob, category);

            // The visible slot itself contains an item, so JEI clicks and R/U query the egg.
            // Only the renderer uses the original mob, preserving its model and recipe NBT.
            slot.setCustomRenderer(VanillaTypes.ITEM_STACK, new IIngredientRenderer<ItemStack>() {
                @Override
                public void render(GuiGraphics graphics, ItemStack ingredient) {
                    if (egg != null) {
                        entityRenderer.render(graphics, mob);
                    } else {
                        graphics.renderItem(ingredient, 0, 0);
                        graphics.renderItemDecorations(Minecraft.getInstance().font, ingredient, 0, 0);
                    }
                }

                @Override
                public List<Component> getTooltip(ItemStack ingredient, TooltipFlag flag) {
                    return List.of(tooltip);
                }

                @Override
                public int getWidth() {
                    return entityRenderer.getWidth();
                }

                @Override
                public int getHeight() {
                    return entityRenderer.getHeight();
                }
            });
            return slot.addItemStack(stack);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to configure Summoning Rituals mob output", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static IIngredientRenderer<Object> getEntityRenderer(Object mob, Object category)
            throws ReflectiveOperationException {
        if (category == null) {
            Class<?> rendererClass = Class.forName(
                    "com.almostreliable.summoningrituals.compat.viewer.jei.entity.EntityIngredientJeiRenderer",
                    true, mob.getClass().getClassLoader());
            return (IIngredientRenderer<Object>) rendererClass.getField("OUTPUT_RENDERER").get(null);
        }
        Field rendererField = category.getClass().getDeclaredField("mobRenderer");
        rendererField.setAccessible(true);
        return (IIngredientRenderer<Object>) rendererField.get(category);
    }

    private static Object invoke(Object target, String name) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(name);
        method.trySetAccessible();
        return method.invoke(target);
    }
}
