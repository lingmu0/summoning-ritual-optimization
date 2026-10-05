package net.xuwu.summoning_ritual_optimization.compat.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public final class JeiMobOutputHelper {
    private static final String LAYOUT_BUILDER = "mezz.jei.api.gui.builder.IRecipeLayoutBuilder";
    private static final String SLOT_BUILDER = "mezz.jei.api.gui.builder.IRecipeSlotBuilder";
    private static final String INGREDIENT_ROLE = "mezz.jei.api.recipe.RecipeIngredientRole";

    private JeiMobOutputHelper() {
    }

    public static void addMobOutput(Object builder, int x, int y, EntityType<?> entityType, int count) {
        ItemStack icon;
        SpawnEggItem egg = SpawnEggItem.byId(entityType);
        if (egg == null) {
            icon = Items.STRUCTURE_VOID.getDefaultInstance();
        } else {
            icon = new ItemStack(egg);
        }

        icon.set(DataComponents.CUSTOM_NAME, Component.translatable(entityType.getDescriptionId())
                .append(Component.literal(" × " + count)));
        addOutputStack(builder, x, y, icon);
    }

    public static void addSummoningRituals1211Outputs(Object builder, Object recipeHolder) {
        try {
            Object recipe = invoke(recipeHolder, "value");
            Object outputs = invoke(recipe, "outputs");
            List<?> itemOutputs = (List<?>) invoke(outputs, "itemOutputs");
            List<?> entityOutputs = (List<?>) invoke(outputs, "entityOutputs");

            for (int i = 0; i < entityOutputs.size(); i++) {
                Object entityInfo = invoke(entityOutputs.get(i), "entityInfo");
                Object entityHolder = invoke(entityInfo, "entity");
                EntityType<?> entityType = (EntityType<?>) invoke(entityHolder, "value");
                int count = ((Number) invoke(entityInfo, "count")).intValue();
                int slotIndex = itemOutputs.size() + i;
                addMobOutput(builder, 2 + slotIndex * 16, 130, entityType, count);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read Summoning Rituals 1.21.1 outputs", exception);
        }
    }

    public static void addGoety1211Output(Object builder, Object recipe) {
        try {
            Object entityType = invoke(recipe, "getEntityToSummon");
            if (entityType == null) {
                entityType = invoke(recipe, "getEntityToConvertInto");
            }
            if (entityType instanceof EntityType<?> entity) {
                addMobOutput(builder, 149, 57, entity, 1);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read Goety 3.2.0 ritual output", exception);
        }
    }

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        Objects.requireNonNull(target, "target");
        Method method = target.getClass().getMethod(methodName);
        method.trySetAccessible();
        return method.invoke(target);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addOutputStack(Object builder, int x, int y, ItemStack stack) {
        try {
            ClassLoader classLoader = builder.getClass().getClassLoader();
            Class<?> roleType = Class.forName(INGREDIENT_ROLE, true, classLoader);
            Object outputRole = Enum.valueOf((Class<? extends Enum>) roleType, "OUTPUT");
            Class<?> layoutBuilderType = Class.forName(LAYOUT_BUILDER, true, classLoader);
            Object slot = layoutBuilderType.getMethod("addSlot", roleType, int.class, int.class)
                    .invoke(builder, outputRole, x, y);
            Class<?> slotBuilderType = Class.forName(SLOT_BUILDER, true, classLoader);
            slotBuilderType.getMethod("addItemStack", ItemStack.class).invoke(slot, stack);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to add JEI mob output slot", exception);
        }
    }
}
