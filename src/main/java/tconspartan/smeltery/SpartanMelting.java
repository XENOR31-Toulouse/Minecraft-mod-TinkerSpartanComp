package tconspartan.smeltery;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import slimeknights.tconstruct.smeltery.events.TinkerSmelteryEvent;
import net.minecraftforge.oredict.OreDictionary;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.Material;
import tconspartan.TConSpartan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rend fondables dans la smeltery les armes (et munitions) de Spartan Weaponry faites en métal.
 *
 * <p>Plutôt qu'une table figée, on lit les recettes de craft de Spartan : la quantité de métal rendue
 * correspond exactement aux lingots/pépites utilisés, y compris si un pack modifie les recettes ou si Spartan
 * ajoute des matériaux (cuivre, bronze, acier... quand un mod les fournit).</p>
 */
@Mod.EventBusSubscriber(modid = TConSpartan.MODID)
public final class SpartanMelting {

    private static final String SPARTAN = "spartanweaponry";

    private SpartanMelting() {
    }

    /**
     * Usure : une arme Spartan abîmée rend moins de métal, proportionnellement à sa durabilité restante
     * (arrondi au nugget, minimum 1 nugget). Les recettes de fonte restent celles d'une arme neuve (affichage JEI).
     */
    @SubscribeEvent
    public static void onMelting(TinkerSmelteryEvent.OnMelting event) {
        ItemStack stack = event.itemStack;
        if (event.result == null || stack.isEmpty() || !stack.isItemStackDamageable() || stack.getMaxDamage() <= 0
                || stack.getItem().getRegistryName() == null
                || !SPARTAN.equals(stack.getItem().getRegistryName().getNamespace())) {
            return;
        }
        float remaining = 1f - (float) stack.getItemDamage() / stack.getMaxDamage();
        int nuggets = (int) (event.result.amount * remaining / Material.VALUE_Nugget);
        event.result.amount = Math.max(1, nuggets) * Material.VALUE_Nugget;
    }

    public static void registerAll() {
        int count = 0;
        Set<Item> done = new HashSet<>();

        for (IRecipe recipe : ForgeRegistries.RECIPES) {
            ItemStack output = recipe.getRecipeOutput();
            if (output.isEmpty() || output.getItem().getRegistryName() == null
                    || !SPARTAN.equals(output.getItem().getRegistryName().getNamespace())
                    || done.contains(output.getItem())) {
                continue;
            }

            MetalContent metal = MetalContent.of(recipe);
            if (metal == null) {
                continue;
            }
            int perItem = metal.amount / output.getCount();
            if (perItem < Material.VALUE_Nugget) {
                continue; // ex. flèches en fer : trop peu de métal par unité
            }

            // WILDCARD : les armes abîmées fondent aussi.
            ItemStack input = new ItemStack(output.getItem(), 1,
                    output.getItem().isDamageable() ? OreDictionary.WILDCARD_VALUE : output.getMetadata());
            TinkerRegistry.registerMelting(input, metal.fluid, perItem);
            done.add(output.getItem());
            count++;
        }
        TConSpartan.LOG.info("Registered {} smeltery melting recipes for Spartan Weaponry items", count);
    }

    /** Le métal (fluide Tinkers) et la quantité en mB contenus dans une recette. */
    private static final class MetalContent {
        private static final Map<String, Integer> PREFIXES = new HashMap<>();

        static {
            PREFIXES.put("block", Material.VALUE_Block);
            PREFIXES.put("ingot", Material.VALUE_Ingot);
            PREFIXES.put("nugget", Material.VALUE_Nugget);
        }

        final Fluid fluid;
        final int amount;

        private MetalContent(Fluid fluid, int amount) {
            this.fluid = fluid;
            this.amount = amount;
        }

        /** null si la recette ne contient pas de métal, ou mélange plusieurs métaux. */
        static MetalContent of(IRecipe recipe) {
            Fluid fluid = null;
            int amount = 0;
            for (Ingredient ingredient : recipe.getIngredients()) {
                ItemStack[] stacks = ingredient.getMatchingStacks();
                if (stacks.length == 0) {
                    continue;
                }
                MetalContent part = fromStack(stacks[0]);
                if (part == null) {
                    continue; // bâton, cuir, ficelle...
                }
                if (fluid != null && fluid != part.fluid) {
                    return null; // alliage de plusieurs métaux : ambigu, on ignore
                }
                fluid = part.fluid;
                amount += part.amount;
            }
            return fluid != null && amount > 0 ? new MetalContent(fluid, amount) : null;
        }

        private static MetalContent fromStack(ItemStack stack) {
            if (stack.isEmpty()) {
                return null;
            }
            for (int id : OreDictionary.getOreIDs(stack)) {
                String ore = OreDictionary.getOreName(id);
                for (Map.Entry<String, Integer> prefix : PREFIXES.entrySet()) {
                    if (ore.startsWith(prefix.getKey()) && ore.length() > prefix.getKey().length()) {
                        // "ingotIron" -> "iron", "ingotElectrum" -> "electrum" (noms des fluides Tinkers)
                        String metal = ore.substring(prefix.getKey().length()).toLowerCase(Locale.ROOT);
                        Fluid fluid = FluidRegistry.getFluid(metal);
                        if (fluid != null) {
                            return new MetalContent(fluid, prefix.getValue());
                        }
                    }
                }
            }
            return null;
        }
    }
}
