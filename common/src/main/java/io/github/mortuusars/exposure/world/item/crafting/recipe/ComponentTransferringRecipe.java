package io.github.mortuusars.exposure.world.item.crafting.recipe;

import io.github.mortuusars.exposure.Exposure;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ComponentTransferringRecipe extends CustomRecipe {
    private final Ingredient sourceIngredient;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStackTemplate result;
    private final CraftingBookCategory category;

    public ComponentTransferringRecipe(CraftingBookCategory category, Ingredient sourceIngredient,
                                      NonNullList<Ingredient> ingredients, ItemStackTemplate result) {
        super();
        this.category = category;
        this.sourceIngredient = sourceIngredient;
        this.ingredients = ingredients;
        this.result = result;
    }

    @Override
    public @NotNull RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return Exposure.RecipeSerializers.COMPONENT_TRANSFERRING.get();
    }

    @Override
    public @NotNull CraftingBookCategory category() {
        return category;
    }

    public @NotNull Ingredient getSourceIngredient() {
        return sourceIngredient;
    }

    public @NotNull NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public @NotNull ItemStack getResultItem() {
        return getResult();
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        List<Ingredient> placementIngredients = new ArrayList<>(ingredients.size() + 1);
        placementIngredients.add(sourceIngredient);
        placementIngredients.addAll(ingredients);
        return PlacementInfo.create(placementIngredients);
    }

    public @NotNull ItemStack getResult() {
        return result.create();
    }

    public @NotNull ItemStackTemplate getResultTemplate() {
        return result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (getSourceIngredient().isEmpty() || ingredients.isEmpty())
            return false;

        List<Ingredient> unmatchedIngredients = new ArrayList<>(ingredients);
        unmatchedIngredients.addFirst(getSourceIngredient());

        int itemsInCraftingGrid = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty())
                itemsInCraftingGrid++;

            if (itemsInCraftingGrid > ingredients.size() + 1)
                return false;

            if (!unmatchedIngredients.isEmpty()) {
                for (int j = 0; j < unmatchedIngredients.size(); j++) {
                    if (unmatchedIngredients.get(j).test(stack)) {
                        unmatchedIngredients.remove(j);
                        break;
                    }
                }
            }
        }

        return unmatchedIngredients.isEmpty() && itemsInCraftingGrid == ingredients.size() + 1;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput input) {
        for (int index = 0; index < input.size(); index++) {
            ItemStack itemStack = input.getItem(index);

            if (getSourceIngredient().test(itemStack)) {
                return transferComponents(itemStack, getResultItem().copy());
            }
        }

        return getResultItem().copy();
    }

    public @NotNull ItemStack transferComponents(ItemStack transferIngredientStack, ItemStack recipeResultStack) {
        /*
         * 26.1 item prototypes include identity-facing defaults such as ITEM_MODEL and ITEM_NAME.
         * Copying the resolved component map transfers those source defaults to a different result item
         * (for example, an undeveloped film model onto developed film). Only the source stack's patch is
         * authored state and belongs on the recipe result; the result item's own prototype remains intact.
         */
        recipeResultStack.applyComponents(transferIngredientStack.getComponentsPatch());
        return recipeResultStack;
    }

    public boolean canCraftInDimensions(int width, int height) {
        return ingredients.size() <= width * height;
    }
}
