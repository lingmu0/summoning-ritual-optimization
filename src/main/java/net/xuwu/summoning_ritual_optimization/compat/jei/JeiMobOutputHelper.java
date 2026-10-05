package net.xuwu.summoning_ritual_optimization.compat.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

        icon.setHoverName(Component.translatable(entityType.getDescriptionId())
                .append(Component.literal(" × " + count)));
        addOutputStack(builder, x, y, icon);
    }

    public static void addSummoningRituals1201Outputs(Object builder, Object recipe) {
        try {
            Object outputs = invoke(recipe, "getOutputs");
            ClassLoader classLoader = outputs.getClass().getClassLoader();
            Class<?> consumerType = Class.forName("org.apache.logging.log4j.util.TriConsumer", true, classLoader);
            Object consumer = Proxy.newProxyInstance(classLoader, new Class<?>[]{consumerType}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return objectMethod(proxy, method.getName(), args);
                }
                if (args != null && args.length == 3 && "MOB".equals(String.valueOf(args[0]))) {
                    Object mobOutput = args[1];
                    EntityType<?> entityType = (EntityType<?>) invoke(mobOutput, "getOutput");
                    int count = ((Number) invoke(mobOutput, "getCount")).intValue();
                    int index = ((Number) args[2]).intValue();
                    addMobOutput(builder, 2 + index * 17, 130, entityType, count);
                }
                return null;
            });

            Method forEach = outputs.getClass().getMethod("forEach", consumerType);
            forEach.invoke(outputs, consumer);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read Summoning Rituals 1.20.1 outputs", exception);
        }
    }

    public static void addGoety1201Output(Object builder, Object recipe) {
        try {
            EntityType<?> entityType = null;
            if (Boolean.TRUE.equals(invoke(recipe, "isSummoning"))) {
                entityType = (EntityType<?>) invoke(recipe, "getEntityToSummon");
            } else if (Boolean.TRUE.equals(invoke(recipe, "isConversion"))) {
                entityType = (EntityType<?>) invoke(recipe, "getEntityToConvertInto");
            }

            if (entityType != null) {
                addMobOutput(builder, 149, 57, entityType, 1);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read Goety 1.20.1 ritual output", exception);
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

    private static Object objectMethod(Object proxy, String name, Object[] args) {
        return switch (name) {
            case "toString" -> "MobOutputConsumer";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null ? null : args[0]);
            default -> null;
        };
    }
}
