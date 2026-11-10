package tconspartan;

import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.registries.IForgeRegistry;
import slimeknights.tconstruct.library.TinkerRegistry;
import tconspartan.throwing.ThrownTools;
import tconspartan.tools.SpartanTool;
import tconspartan.tools.SpartanWeapon;

/**
 * Enregistre les outils et les projectiles.
 *
 * <p>Les outils n'utilisent que des pièces existantes de Tinkers. Le constructeur de {@code ToolCore} inscrit
 * l'outil dans le TinkerRegistry (et ses pièces, déjà connues de Tinkers).</p>
 */
@Mod.EventBusSubscriber(modid = TConSpartan.MODID)
public final class ModTools {

    private ModTools() {
    }

    // LOW : on passe après Tinkers, dont on réutilise les pièces.
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        for (SpartanWeapon weapon : SpartanWeapon.values()) {
            SpartanTool tool = new SpartanTool(weapon);
            tool.setTranslationKey(TConSpartan.MODID + "." + weapon.id());
            tool.setRegistryName(TConSpartan.MODID, weapon.id());
            registry.register(tool);
            weapon.tool = tool;
        }
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        IForgeRegistry<EntityEntry> registry = event.getRegistry();
        int id = 0;
        registry.register(entity(ThrownTools.Dagger.class, "thrown_dagger", id++));
        registry.register(entity(ThrownTools.Knife.class, "thrown_knife", id++));
        registry.register(entity(ThrownTools.Axe.class, "thrown_axe", id++));
        registry.register(entity(ThrownTools.Javelin.class, "thrown_javelin", id++));
        registry.register(entity(ThrownTools.Boomerang.class, "thrown_boomerang", id));
    }

    private static EntityEntry entity(Class<? extends Entity> type, String name, int id) {
        return EntityEntryBuilder.create()
                .entity(type)
                .id(new ResourceLocation(TConSpartan.MODID, name), id)
                .name(TConSpartan.MODID + "." + name)
                .tracker(64, 20, true)
                .build();
    }

    /** Appelé en init : rend les outils fabricables à la Tool Station / Tool Forge. */
    static void registerToolBuilding() {
        for (SpartanWeapon weapon : SpartanWeapon.values()) {
            if (weapon.forgeOnly()) {
                TinkerRegistry.registerToolForgeCrafting(weapon.tool);
            } else {
                TinkerRegistry.registerToolCrafting(weapon.tool);
            }
        }
    }
}
