package tconspartan.throwing;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import slimeknights.tconstruct.library.utils.ToolHelper;
import tconspartan.tools.SpartanTool;

import java.util.UUID;

/**
 * Munitions des armes de lancer (comme Spartan : 16 couteaux, 8 haches, 4 javelots par pile).
 *
 * <ul>
 *     <li>Une pile sans tag est pleine : un outil tout juste fabriqué contient le maximum.</li>
 *     <li>Lancer retire une munition ; le projectile emporte une copie de l'outil marquée avec l'identifiant
 *     de sa pile d'origine et l'usure qu'il avait au départ.</li>
 *     <li>Au ramassage, le projectile retourne dans sa pile d'origine (si elle n'est pas pleine) et lui transmet
 *     l'usure subie pendant le vol. Si l'usure dépasse la durabilité restante, ce projectile-là est perdu
 *     (comme Spartan) au lieu de casser toute la pile.</li>
 * </ul>
 */
public final class Ammo {

    private static final String TAG_AMMO = "TConSpartanAmmo";
    private static final String TAG_ID = "TConSpartanStack";
    private static final String TAG_THROWN_DAMAGE = "TConSpartanThrownDamage";

    private Ammo() {
    }

    public static int max(ItemStack stack) {
        return stack.getItem() instanceof SpartanTool ? ((SpartanTool) stack.getItem()).getWeapon().maxAmmo() : 1;
    }

    /** Nombre de projectiles dans la pile (au moins 1). */
    public static int get(ItemStack stack) {
        int max = max(stack);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_AMMO)) {
            return max;
        }
        return Math.max(1, Math.min(max, tag.getInteger(TAG_AMMO)));
    }

    public static void set(ItemStack stack, int ammo) {
        tag(stack).setInteger(TAG_AMMO, ammo);
    }

    /** Copie de l'outil emportée par un projectile (1 munition, liée à la pile d'origine). */
    public static ItemStack splitForThrow(ItemStack stack) {
        NBTTagCompound tag = tag(stack);
        if (!tag.hasUniqueId(TAG_ID)) {
            tag.setUniqueId(TAG_ID, UUID.randomUUID());
        }
        ItemStack projectile = stack.copy();
        projectile.setCount(1);
        set(projectile, 1);
        tag(projectile).setInteger(TAG_THROWN_DAMAGE, projectile.getItemDamage());
        return projectile;
    }

    /**
     * Remet un projectile ramassé dans sa pile d'origine.
     *
     * @return false si aucune pile d'origine non pleine n'a été trouvée (le projectile devient alors un objet à part).
     */
    public static boolean returnToStack(EntityPlayer player, ItemStack projectile) {
        NBTTagCompound projectileTag = projectile.getTagCompound();
        if (projectileTag == null || !projectileTag.hasUniqueId(TAG_ID)) {
            return false;
        }
        UUID id = projectileTag.getUniqueId(TAG_ID);

        for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
            ItemStack stack = player.inventory.getStackInSlot(slot);
            NBTTagCompound tag = stack.getTagCompound();
            if (stack.getItem() != projectile.getItem() || tag == null || !tag.hasUniqueId(TAG_ID)
                    || !id.equals(tag.getUniqueId(TAG_ID)) || get(stack) >= max(stack)) {
                continue;
            }

            int wear = Math.max(0, projectile.getItemDamage() - projectileTag.getInteger(TAG_THROWN_DAMAGE));
            int maxDamage = stack.getMaxDamage();
            int total = stack.getItemDamage() + wear;
            if (ToolHelper.isBroken(projectile) || total >= maxDamage) {
                // Ce projectile est usé jusqu'au bout : on le perd, la pile repart avec le surplus d'usure.
                player.renderBrokenItemStack(projectile);
                stack.setItemDamage(Math.min(maxDamage - 1, Math.max(0, total - maxDamage)));
            } else {
                set(stack, get(stack) + 1);
                stack.setItemDamage(total);
            }
            return true;
        }
        return false;
    }

    /** Un projectile ramassé sans pile d'origine devient un objet normal (1 munition). */
    public static void clearThrowMarker(ItemStack projectile) {
        NBTTagCompound tag = projectile.getTagCompound();
        if (tag != null) {
            tag.removeTag(TAG_THROWN_DAMAGE);
        }
    }

    private static NBTTagCompound tag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
