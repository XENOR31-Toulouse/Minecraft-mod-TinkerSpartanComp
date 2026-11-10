package tconspartan.tools;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import slimeknights.tconstruct.library.utils.ToolHelper;
import tconspartan.TConSpartan;

/**
 * Fournit à Spartan les "dégâts directs" de l'outil Tinkers en cours d'utilisation.
 *
 * <p>Le handler de Spartan (priorité NORMAL) appelle {@code getDirectAttackDamage()} sans ItemStack. On mémorise
 * donc l'attaque réelle de l'outil juste avant (HIGHEST) et on l'efface juste après (LOWEST).</p>
 */
@Mod.EventBusSubscriber(modid = TConSpartan.MODID)
public final class AttackContext {

    private static final ThreadLocal<Float> CURRENT = new ThreadLocal<>();

    private AttackContext() {
    }

    static float currentAttackDamage() {
        Float value = CURRENT.get();
        // Hors contexte : valeur haute = Spartan utilise les dégâts de l'événement tels quels.
        return value != null ? value : Float.MAX_VALUE / 2f;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeSpartan(LivingHurtEvent event) {
        Entity source = event.getSource().getTrueSource();
        if (source instanceof EntityLivingBase) {
            ItemStack held = ((EntityLivingBase) source).getHeldItemMainhand();
            if (held.getItem() instanceof SpartanTool) {
                CURRENT.set(ToolHelper.getActualAttack(held));
                return;
            }
        }
        CURRENT.remove();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void afterSpartan(LivingHurtEvent event) {
        CURRENT.remove();
    }
}
