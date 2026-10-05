package net.xuwu.summoning_ritual_optimization.compat.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class JeiMobOutputHelper {
    private static final String LAYOUT_BUILDER = "mezz.jei.api.gui.builder.IRecipeLayoutBuilder";
    private static final String SLOT_BUILDER = "mezz.jei.api.gui.builder.IRecipeSlotBuilder";
    private static final String INGREDIENT_ROLE = "mezz.jei.api.recipe.RecipeIngredientRole";
    private static final String INGREDIENT_RENDERER = "mezz.jei.api.ingredients.IIngredientRenderer";
    private static final String VANILLA_TYPES = "mezz.jei.api.constants.VanillaTypes";
    private static final Map<EntityType<?>, Optional<SpawnEggItem>> SPAWN_EGGS = new HashMap<>();

    private JeiMobOutputHelper() {
    }

    public static void addMobOutput(Object builder, int x, int y, EntityType<?> entityType, int count) {
        Component mobTooltip = Component.translatable(entityType.getDescriptionId())
                .append(Component.literal(" \u00D7 " + count));
        SpawnEggItem egg = findSpawnEgg(entityType);
        if (egg == null) {
            ItemStack placeholder = Items.STRUCTURE_VOID.getDefaultInstance();
            placeholder.setHoverName(mobTooltip);
            addOutputStack(builder, x, y, placeholder, null);
        } else {
            addOutputStack(builder, x, y, new ItemStack(egg), mobTooltip);
        }
    }

    private static SpawnEggItem findSpawnEgg(EntityType<?> entityType) {
        return SPAWN_EGGS.computeIfAbsent(entityType, type -> {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof SpawnEggItem egg && egg.getType(null) == type) {
                    return Optional.of(egg);
                }
            }
            return Optional.ofNullable(SpawnEggItem.byId(type));
        }).orElse(null);
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
    private static void addOutputStack(Object builder, int x, int y, ItemStack stack, Component hiddenEggTooltip) {
        try {
            ClassLoader classLoader = builder.getClass().getClassLoader();
            Class<?> roleType = Class.forName(INGREDIENT_ROLE, true, classLoader);
            Object outputRole = Enum.valueOf((Class<? extends Enum>) roleType, "OUTPUT");
            Class<?> layoutBuilderType = Class.forName(LAYOUT_BUILDER, true, classLoader);
            Object slot = layoutBuilderType.getMethod("addSlot", roleType, int.class, int.class)
                    .invoke(builder, outputRole, x, y);
            Class<?> slotBuilderType = Class.forName(SLOT_BUILDER, true, classLoader);
            if (hiddenEggTooltip != null) {
                Class<?> ingredientRendererType = Class.forName(INGREDIENT_RENDERER, true, classLoader);
                Class<?> vanillaTypes = Class.forName(VANILLA_TYPES, true, classLoader);
                Object itemStackIngredientType = vanillaTypes.getField("ITEM_STACK").get(null);
                Object renderer = createHiddenEggRenderer(classLoader, ingredientRendererType, hiddenEggTooltip);
                slotBuilderType.getMethod("setCustomRenderer",
                                Class.forName("mezz.jei.api.ingredients.IIngredientType", true, classLoader),
                                ingredientRendererType)
                        .invoke(slot, itemStackIngredientType, renderer);
            }
            slotBuilderType.getMethod("addItemStack", ItemStack.class).invoke(slot, stack);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to add JEI mob output slot", exception);
        }
    }

    private static Object createHiddenEggRenderer(ClassLoader classLoader, Class<?> rendererType, Component tooltip) {
        InvocationHandler handler = (proxy, method, args) -> {
            return switch (method.getName()) {
                case "render" -> null;
                case "getTooltip" -> method.getReturnType() == List.class
                        ? List.of(tooltip)
                        : InvocationHandler.invokeDefault(proxy, method, args);
                case "getWidth", "getHeight" -> 16;
                case "toString" -> "HiddenSpawnEggRenderer[" + tooltip.getString() + "]";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> method.isDefault() ? InvocationHandler.invokeDefault(proxy, method, args) : null;
            };
        };
        return Proxy.newProxyInstance(classLoader, new Class<?>[]{rendererType}, handler);
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
