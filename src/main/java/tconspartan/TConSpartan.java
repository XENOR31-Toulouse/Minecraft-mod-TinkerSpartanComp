package tconspartan;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tconspartan.proxy.CommonProxy;
import tconspartan.smeltery.SpartanMelting;

/**
 * Addon de liaison Tinkers' Construct <-> Spartan Weaponry.
 *
 * - Les armes de Spartan deviennent des outils modulaires Tinkers (voir {@link tconspartan.tools.SpartanWeapon}).
 * - Les armes Spartan en métal peuvent être fondues dans la smeltery ({@link SpartanMelting}).
 */
@Mod(
        modid = TConSpartan.MODID,
        name = TConSpartan.NAME,
        version = TConSpartan.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:mantle;required-after:tconstruct;required-after:spartanweaponry"
)
public class TConSpartan {

    public static final String MODID = "tconspartan";
    public static final String NAME = "Tinkers' Spartan";
    public static final String VERSION = "0.1.0";

    public static final Logger LOG = LogManager.getLogger(NAME);

    @SidedProxy(clientSide = "tconspartan.proxy.ClientProxy", serverSide = "tconspartan.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModTools.registerToolBuilding();
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Les recettes et fluides de tous les mods sont en place : on peut calculer les fontes.
        SpartanMelting.registerAll();
    }
}
