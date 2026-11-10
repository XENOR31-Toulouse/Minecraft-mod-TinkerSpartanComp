package tconspartan.proxy;

import com.oblivioussp.spartanweaponry.client.render.projectile.RenderFactoryBoomerang;
import com.oblivioussp.spartanweaponry.client.render.projectile.RenderFactoryThrowingAxe;
import com.oblivioussp.spartanweaponry.client.render.projectile.RenderFactoryThrowingKnife;
import com.oblivioussp.spartanweaponry.client.render.projectile.RenderFactoryThrownJavelin;
import com.oblivioussp.spartanweaponry.client.render.projectile.RenderFactoryThrownWeapon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import slimeknights.tconstruct.common.ModelRegisterUtil;
import slimeknights.tconstruct.library.TinkerRegistryClient;
import slimeknights.tconstruct.library.client.ToolBuildGuiInfo;
import slimeknights.tconstruct.library.client.model.ModifierModelLoader;
import tconspartan.TConSpartan;
import tconspartan.client.SpartanBook;
import tconspartan.client.SpartanTextures;
import tconspartan.throwing.ThrownTools;
import tconspartan.tools.SpartanWeapon;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = TConSpartan.MODID, value = Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        // Projectiles : on réutilise les rendus de Spartan (ils dessinent l'objet porté par le projectile).
        RenderingRegistry.registerEntityRenderingHandler(ThrownTools.Dagger.class, new RenderFactoryThrownWeapon<>());
        RenderingRegistry.registerEntityRenderingHandler(ThrownTools.Knife.class, new RenderFactoryThrowingKnife());
        RenderingRegistry.registerEntityRenderingHandler(ThrownTools.Axe.class, new RenderFactoryThrowingAxe<>());
        RenderingRegistry.registerEntityRenderingHandler(ThrownTools.Javelin.class, new RenderFactoryThrownJavelin());
        RenderingRegistry.registerEntityRenderingHandler(ThrownTools.Boomerang.class, new RenderFactoryBoomerang());
    }

    @Override
    public void init() {
        // Emplacements des pièces dans la Tool Station / Tool Forge : manche, tête, [garde/attache].
        for (SpartanWeapon weapon : SpartanWeapon.values()) {
            ToolBuildGuiInfo info = new ToolBuildGuiInfo(weapon.tool);
            info.addSlotPosition(12, 62);
            if (weapon.hasExtraPart()) {
                info.addSlotPosition(48, 26);
                info.addSlotPosition(30, 44);
            } else {
                info.addSlotPosition(34, 36);
            }
            TinkerRegistryClient.addToolBuilding(info);
        }
        // Section « Arsenal spartiate » dans le livre de Tinkers
        SpartanBook.register();
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (SpartanWeapon weapon : SpartanWeapon.values()) {
            // -> models/item/tools/<arme>.tcon.json
            ModelRegisterUtil.registerToolModel(weapon.tool);
        }
        registerModifierModels();
    }

    /**
     * Ajoute nos fichiers models/item/modifiers/<modificateur>.json (texture de chaque modificateur pour chacune
     * de nos armes) au chargeur de Tinkers. On passe par son chargeur directement : l'API publique demande l'objet
     * modificateur, et "fortify" n'en a pas dans le registre.
     */
    private static void registerModifierModels() {
        try {
            Field field = slimeknights.tconstruct.common.ClientProxy.class.getDeclaredField("modifierLoader");
            field.setAccessible(true);
            ModifierModelLoader loader = (ModifierModelLoader) field.get(null);
            for (String modifier : SpartanTextures.MODIFIERS) {
                loader.registerModifierFile(modifier,
                        new ResourceLocation(TConSpartan.MODID, "models/item/modifiers/" + modifier));
            }
        } catch (ReflectiveOperationException | ClassCastException e) {
            TConSpartan.LOG.error("Impossible d'enregistrer les rendus de modificateurs", e);
        }
    }

    @SubscribeEvent
    public static void stitchTextures(TextureStitchEvent.Pre event) {
        SpartanTextures.register(event.getMap());
    }
}
