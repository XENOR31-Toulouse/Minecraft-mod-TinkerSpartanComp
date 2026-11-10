package tconspartan.throwing;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import slimeknights.tconstruct.library.utils.AmmoHelper;
import slimeknights.tconstruct.library.utils.ToolHelper;
import tconspartan.tools.SpartanTool;

/**
 * Munitions des armes de lancer, calculées comme les flèches et shurikens de Tinkers ({@code ProjectileCore}) :
 *
 * <ul>
 *     <li>la <b>durabilité est la réserve de munitions</b> : nombre = durabilité / « durabilité par projectile ».
 *     La durabilité vient des matériaux des pièces (tête, manche), donc le nombre de projectiles aussi : avec la
 *     config par défaut, du fer donne 15 couteaux / 8 haches / 4 javelots, du silex sur bois 11 couteaux,
 *     du cobalt 53 couteaux ;</li>
 *     <li>lancer consomme une munition ; à 0 l'outil reste dans l'inventaire, « vide » (cassé) ;</li>
 *     <li>un projectile ramassé rend une munition à une pile identique (mêmes matériaux et modificateurs, comme
 *     les flèches Tinkers) ou devient une pile d'une munition ;</li>
 *     <li>chaque impact use le projectile lui-même : après assez de coups, il est perdu.</li>
 * </ul>
 */
public final class Ammo {

    private Ammo() {
    }

    /** Durabilité consommée par projectile, ou 0 si l'arme n'a pas de munitions. */
    public static int perAmmo(ItemStack stack) {
        return stack.getItem() instanceof SpartanTool ? ((SpartanTool) stack.getItem()).getWeapon().durabilityPerAmmo() : 0;
    }

    public static boolean usesAmmo(ItemStack stack) {
        return perAmmo(stack) > 0;
    }

    public static int max(ItemStack stack) {
        int per = perAmmo(stack);
        return per > 0 ? Math.max(1, ToolHelper.getMaxDurability(stack) / per) : 1;
    }

    public static int get(ItemStack stack) {
        int per = perAmmo(stack);
        if (per <= 0) {
            return 1;
        }
        return ToolHelper.isBroken(stack) ? 0 : ToolHelper.getCurrentDurability(stack) / per;
    }

    /** Fixe le nombre de munitions (équivalent de ProjectileCore#setAmmo). */
    public static void set(ItemStack stack, int count) {
        stack.setItemDamage(Math.max(0, (max(stack) - count) * perAmmo(stack)));
    }

    /** Consomme une munition ; l'outil devient « vide » (cassé) quand il n'en reste plus. */
    public static void use(ItemStack stack, EntityLivingBase thrower) {
        int before = get(stack);
        if (before <= 0) {
            return;
        }
        ToolHelper.damageTool(stack, perAmmo(stack), thrower);
        if (get(stack) <= 0 && !ToolHelper.isBroken(stack)) {
            ToolHelper.breakTool(stack, thrower);
        }
    }

    /** Copie de l'outil portée par le projectile : exactement une munition. */
    public static ItemStack forProjectile(ItemStack stack) {
        ItemStack projectile = stack.copy();
        projectile.setCount(1);
        ToolHelper.unbreakTool(projectile);
        set(projectile, 1);
        return projectile;
    }

    /**
     * Ramassage d'un projectile : rend sa munition à une pile identique de l'inventaire.
     *
     * @return true si le projectile a été absorbé (rendu à une pile, ou usé jusqu'au bout et donc perdu) ;
     * false s'il doit être ajouté à l'inventaire comme nouvelle pile.
     */
    public static boolean returnToStack(EntityLivingBase entity, ItemStack projectile) {
        if (get(projectile) <= 0) {
            return true; // usé par ses impacts : il ne reste rien à ramasser
        }
        ItemStack target = AmmoHelper.getMatchingItemstackFromInventory(projectile, entity, true);
        if (target.isEmpty() || get(target) >= max(target)) {
            return false;
        }
        ToolHelper.unbreakTool(target);
        ToolHelper.healTool(target, perAmmo(target), null);
        return true;
    }
}
