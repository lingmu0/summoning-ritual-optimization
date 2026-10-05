package net.xuwu.summoning_ritual_optimization.compat.jei;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
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

    public static void addMobOutput(Object builder, int x, int y, EntityType<?> entityType, int count,
                                    boolean hideSpawnEggTexture) {
        Component mobTooltip = Component.translatable(entityType.getDescriptionId())
                .append(Component.literal(" \u00D7 " + count));
        SpawnEggItem egg = findSpawnEgg(entityType);
        if (egg == null) {
            ItemStack placeholder = Items.STRUCTURE_VOID.getDefaultInstance();
            placeholder.set(DataComponents.CUSTOM_NAME, mobTooltip);
            addOutputStack(builder, x, y, placeholder, null, false);
        } else {
            Object eggSlot = addOutputStack(builder, x, y, new ItemStack(egg), mobTooltip, hideSpawnEggTexture);
            if (hideSpawnEggTexture) {
                linkMobAndEggOutputs(builder, eggSlot, egg, entityType);
            }
        }
    }

    private static SpawnEggItem findSpawnEgg(EntityType<?> entityType) {
        return SPAWN_EGGS.computeIfAbsent(entityType, type -> {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof SpawnEggItem egg && egg.getType(new ItemStack(item)) == type) {
                    return Optional.of(egg);
                }
            }
            return Optional.ofNullable(SpawnEggItem.byId(type));
        }).orElse(null);
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
                addMobOutput(builder, 2 + slotIndex * 16, 130, entityType, count, true);
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
                addMobOutput(builder, 149, 57, entity, 1, false);
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
    private static Object addOutputStack(Object builder, int x, int y, ItemStack stack, Component mobTooltip,
                                         boolean hideSpawnEggTexture) {
        try {
            ClassLoader classLoader = builder.getClass().getClassLoader();
            Class<?> roleType = Class.forName(INGREDIENT_ROLE, true, classLoader);
            Object outputRole = Enum.valueOf((Class<? extends Enum>) roleType, "OUTPUT");
            Class<?> layoutBuilderType = Class.forName(LAYOUT_BUILDER, true, classLoader);
            Object slot = layoutBuilderType.getMethod("addSlot", roleType, int.class, int.class)
                    .invoke(builder, outputRole, x, y);
            Class<?> slotBuilderType = Class.forName(SLOT_BUILDER, true, classLoader);
            if (mobTooltip != null) {
                Class<?> ingredientRendererType = Class.forName(INGREDIENT_RENDERER, true, classLoader);
                Class<?> vanillaTypes = Class.forName(VANILLA_TYPES, true, classLoader);
                Object itemStackIngredientType = vanillaTypes.getField("ITEM_STACK").get(null);
                Object renderer = createMobEggRenderer(classLoader, ingredientRendererType, mobTooltip,
                        hideSpawnEggTexture);
                slotBuilderType.getMethod("setCustomRenderer",
                                Class.forName("mezz.jei.api.ingredients.IIngredientType", true, classLoader),
                                ingredientRendererType)
                        .invoke(slot, itemStackIngredientType, renderer);
            }
            slotBuilderType.getMethod("addItemStack", ItemStack.class).invoke(slot, stack);
            return slot;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to add JEI mob output slot", exception);
        }
    }

    private static void linkMobAndEggOutputs(Object builder, Object eggSlot, SpawnEggItem egg,
                                             EntityType<?> entityType) {
        try {
            List<Object> linkedSlots = new ArrayList<>();
            for (Object slot : getVisibleSlots(builder)) {
                if (hasMatchingMobOutput(slot, egg, entityType)) {
                    linkedSlots.add(slot);
                }
            }
            if (linkedSlots.isEmpty()) {
                return;
            }
            linkedSlots.add(eggSlot);

            ClassLoader classLoader = builder.getClass().getClassLoader();
            Class<?> acceptorType = Class.forName("mezz.jei.api.gui.builder.IIngredientAcceptor", true, classLoader);
            Object acceptors = Array.newInstance(acceptorType, linkedSlots.size());
            for (int i = 0; i < linkedSlots.size(); i++) {
                Array.set(acceptors, i, linkedSlots.get(i));
            }
            Class<?> layoutBuilderType = Class.forName(LAYOUT_BUILDER, true, classLoader);
            layoutBuilderType.getMethod("createFocusLink", acceptors.getClass()).invoke(builder, (Object) acceptors);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // The recipe still has a clickable spawn-egg output slot if a JEI implementation changes internals.
        }
    }

    private static Collection<?> getVisibleSlots(Object builder) throws ReflectiveOperationException {
        for (Class<?> current = builder.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getName().equals("visibleSlots") && Collection.class.isAssignableFrom(field.getType())) {
                    field.trySetAccessible();
                    Object value = field.get(builder);
                    if (value instanceof Collection<?> slots) {
                        return slots;
                    }
                }
            }
        }
        return List.of();
    }

    private static boolean hasMatchingMobOutput(Object slot, SpawnEggItem egg, EntityType<?> entityType)
            throws ReflectiveOperationException {
        if (!"OUTPUT".equals(String.valueOf(invoke(slot, "getRole")))) {
            return false;
        }
        Object ingredientAcceptor = invoke(slot, "getIngredientAcceptor");
        Object ingredients = invoke(ingredientAcceptor, "getAllIngredients");
        if (!(ingredients instanceof Iterable<?> typedIngredients)) {
            return false;
        }
        for (Object typedIngredient : typedIngredients) {
            Object ingredient = invoke(typedIngredient, "getIngredient");
            if (ingredient.getClass().getName().startsWith("com.almostreliable.summoningrituals.")) {
                if (getEntityType(ingredient) == entityType) {
                    return true;
                }
                Object existingEgg = invoke(ingredient, "getEgg");
                if (existingEgg instanceof SpawnEggItem existingSpawnEgg && existingSpawnEgg == egg) {
                    return true;
                }
                if (existingEgg instanceof ItemStack stack && stack.getItem() == egg) {
                    return true;
                }
            }
        }
        return false;
    }

    private static EntityType<?> getEntityType(Object mobIngredient) throws ReflectiveOperationException {
        try {
            return (EntityType<?>) invoke(mobIngredient, "getEntityType");
        } catch (NoSuchMethodException ignored) {
            Object entityInfo = invoke(mobIngredient, "getEntityInfo");
            Object entityHolder = invoke(entityInfo, "entity");
            return (EntityType<?>) invoke(entityHolder, "value");
        }
    }

    private static Object createMobEggRenderer(ClassLoader classLoader, Class<?> rendererType, Component tooltip,
                                               boolean hideTexture) {
        InvocationHandler handler = (proxy, method, args) -> {
            return switch (method.getName()) {
                case "render" -> {
                    if (method.isDefault()) {
                        yield InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    if (!hideTexture) {
                        GuiGraphics graphics = (GuiGraphics) args[0];
                        ItemStack stack = (ItemStack) args[1];
                        graphics.renderItem(stack, 0, 0);
                        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, 0, 0);
                    }
                    yield null;
                }
                case "getTooltip" -> method.getReturnType() == List.class
                        ? List.of(tooltip)
                        : InvocationHandler.invokeDefault(proxy, method, args);
                case "getWidth", "getHeight" -> 16;
                case "toString" -> "MobSpawnEggRenderer[" + tooltip.getString() + "]";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> method.isDefault() ? InvocationHandler.invokeDefault(proxy, method, args) : null;
            };
        };
        return Proxy.newProxyInstance(classLoader, new Class<?>[]{rendererType}, handler);
    }
}
