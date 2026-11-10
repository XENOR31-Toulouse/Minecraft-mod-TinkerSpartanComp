package tconspartan.throwing;

import com.oblivioussp.spartanweaponry.entity.projectile.EntityBoomerang;
import com.oblivioussp.spartanweaponry.entity.projectile.EntityThrowingAxe;
import com.oblivioussp.spartanweaponry.entity.projectile.EntityThrowingKnife;
import com.oblivioussp.spartanweaponry.entity.projectile.EntityThrownJavelin;
import com.oblivioussp.spartanweaponry.entity.projectile.EntityThrownWeapon;
import com.oblivioussp.spartanweaponry.init.SoundRegistry;
import com.oblivioussp.spartanweaponry.util.DamageSourcesSW;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import slimeknights.tconstruct.library.traits.ITrait;
import slimeknights.tconstruct.library.utils.TinkerUtil;
import slimeknights.tconstruct.library.utils.ToolHelper;
import tconspartan.tools.SpartanTool;

import java.util.Collections;
import java.util.List;

/**
 * Projectiles des armes Tinkers lancées.
 *
 * <p>Ce sont des sous-classes des projectiles de Spartan Weaponry : on garde leur vol, leur rendu, le ramassage et
 * le retour du boomerang. Seul l'impact sur une entité est remplacé, pour appliquer la logique Tinkers (traits,
 * usure qui « casse » l'outil au lieu de le détruire) et les propriétés Spartan de l'arme.</p>
 */
public final class ThrownTools {

    private ThrownTools() {
    }

    /** Accès à {@code EntityArrow#arrowHit} (protégé) depuis le code commun. */
    interface ToolProjectile {
        void onToolHit(EntityLivingBase victim);
    }

    /**
     * Gère l'impact sur une entité si le projectile porte un outil Tinkers.
     *
     * @return false si ce n'est pas le cas (le comportement normal de Spartan s'applique alors).
     */
    static boolean onHit(EntityThrownWeapon projectile, RayTraceResult result) {
        Entity target = result.entityHit;
        ItemStack stack = projectile.getWeaponStack();
        if (target == null || projectile.noClip || !(stack.getItem() instanceof SpartanTool)) {
            return false;
        }

        boolean hit = damageTarget(projectile, stack, (SpartanTool) stack.getItem(), target);
        if (hit) {
            projectile.playSound(SoundRegistry.THROWING_WEAPON_HIT, 1f, 1.2f / (projectile.world.rand.nextFloat() * 0.2f + 0.9f));
            if (target instanceof EntityLivingBase) {
                ((ToolProjectile) projectile).onToolHit((EntityLivingBase) target); // ex. le boomerang repart
            }
        }
        if (!hit || !(target instanceof EntityEnderman)) {
            // Rebond, comme les projectiles de Spartan.
            projectile.motionX *= -0.1;
            projectile.motionY *= -0.1;
            projectile.motionZ *= -0.1;
            projectile.rotationYaw += 180f;
            projectile.prevRotationYaw += 180f;
        }
        return true;
    }

    /**
     * Ramassage : un projectile à munitions retourne dans une pile identique, sinon il devient un objet à part.
     * Remplace EntityThrownWeapon#attemptCatch, qui ne sait fusionner que les armes de lancer de Spartan.
     */
    static boolean catchTool(EntityThrownWeapon projectile, EntityPlayer player) {
        if (projectile.world.isRemote || projectile.arrowShake > 0) {
            return false;
        }
        boolean caught = projectile.pickupStatus == EntityArrow.PickupStatus.ALLOWED
                || (projectile.pickupStatus == EntityArrow.PickupStatus.CREATIVE_ONLY && player.capabilities.isCreativeMode);
        if (projectile.pickupStatus == EntityArrow.PickupStatus.ALLOWED) {
            ItemStack stack = projectile.getWeaponStack().copy();
            // Armes à munitions : retour dans une pile identique (comme les flèches Tinkers), sinon nouvelle pile.
            caught = (Ammo.usesAmmo(stack) && Ammo.returnToStack(player, stack))
                    || player.inventory.addItemStackToInventory(stack);
        }
        if (caught) {
            player.onItemPickup(projectile, 1);
            projectile.setDead();
        }
        return caught;
    }

    private static boolean damageTarget(EntityThrownWeapon projectile, ItemStack stack, SpartanTool tool, Entity target) {
        Entity shooter = projectile.shootingEntity;
        EntityLivingBase attacker = shooter instanceof EntityLivingBase ? (EntityLivingBase) shooter : null;
        EntityLivingBase victim = target instanceof EntityLivingBase ? (EntityLivingBase) target : null;
        DamageSource source = shooter instanceof EntityPlayer
                ? DamageSourcesSW.causeThrownWeaponPlayerDamage(projectile, (EntityPlayer) shooter)
                : DamageSourcesSW.causeThrownWeaponMobDamage(projectile, shooter != null ? shooter : projectile);

        boolean broken = ToolHelper.isBroken(stack);
        List<ITrait> traits = broken ? Collections.emptyList() : TinkerUtil.getTraitsOrdered(stack);
        float baseDamage = broken ? 1f : (float) projectile.getDamage();
        float damage = baseDamage;

        if (attacker != null && victim != null && !broken) {
            for (ITrait trait : traits) {
                damage = trait.damage(stack, attacker, victim, baseDamage, damage, false);
            }
            damage = tool.applyThrownProperties(stack, damage, source, attacker, victim, projectile);
            for (ITrait trait : traits) {
                trait.onHit(stack, attacker, victim, damage, false);
            }
        }

        if (projectile.isBurning() && !(target instanceof EntityEnderman)) {
            target.setFire(5);
        }

        float healthBefore = victim != null ? victim.getHealth() : 0f;
        boolean hit = target.attackEntityFrom(source, damage);
        if (hit && attacker != null && victim != null && !broken) {
            float dealt = healthBefore - victim.getHealth();
            for (ITrait trait : traits) {
                trait.afterHit(stack, attacker, victim, dealt, false, true);
            }
            // Usure Tinkers : l'outil devient « cassé » au lieu de disparaître.
            ToolHelper.damageTool(stack, 1, attacker);
        }
        return hit;
    }

    // ------------------------------------------------------------------ entités (une par rendu Spartan)

    public static class Dagger extends EntityThrownWeapon implements ToolProjectile {
        public Dagger(World world) {
            super(world);
        }

        public Dagger(World world, EntityLivingBase thrower) {
            super(world, thrower);
        }

        @Override
        protected void onHit(RayTraceResult result) {
            if (!ThrownTools.onHit(this, result)) {
                super.onHit(result);
            }
        }

        @Override
        public void onToolHit(EntityLivingBase victim) {
            arrowHit(victim);
        }

        @Override
        protected boolean attemptCatch(EntityPlayer player) {
            return catchTool(this, player);
        }
    }

    public static class Knife extends EntityThrowingKnife implements ToolProjectile {
        public Knife(World world) {
            super(world);
        }

        public Knife(World world, EntityLivingBase thrower) {
            super(world, thrower);
        }

        @Override
        protected void onHit(RayTraceResult result) {
            if (!ThrownTools.onHit(this, result)) {
                super.onHit(result);
            }
        }

        @Override
        public void onToolHit(EntityLivingBase victim) {
            arrowHit(victim);
        }

        @Override
        protected boolean attemptCatch(EntityPlayer player) {
            return catchTool(this, player);
        }
    }

    public static class Axe extends EntityThrowingAxe implements ToolProjectile {
        public Axe(World world) {
            super(world);
        }

        public Axe(World world, EntityLivingBase thrower) {
            super(world, thrower);
        }

        @Override
        protected void onHit(RayTraceResult result) {
            if (!ThrownTools.onHit(this, result)) {
                super.onHit(result);
            }
        }

        @Override
        public void onToolHit(EntityLivingBase victim) {
            arrowHit(victim);
        }

        @Override
        protected boolean attemptCatch(EntityPlayer player) {
            return catchTool(this, player);
        }
    }

    public static class Javelin extends EntityThrownJavelin implements ToolProjectile {
        public Javelin(World world) {
            super(world);
        }

        public Javelin(World world, EntityLivingBase thrower) {
            super(world, thrower);
        }

        @Override
        protected void onHit(RayTraceResult result) {
            if (!ThrownTools.onHit(this, result)) {
                super.onHit(result);
            }
        }

        @Override
        public void onToolHit(EntityLivingBase victim) {
            arrowHit(victim);
        }

        @Override
        protected boolean attemptCatch(EntityPlayer player) {
            return catchTool(this, player);
        }
    }

    public static class Boomerang extends EntityBoomerang implements ToolProjectile {
        public Boomerang(World world) {
            super(world);
        }

        public Boomerang(World world, EntityLivingBase thrower) {
            super(world, thrower);
        }

        @Override
        protected void onHit(RayTraceResult result) {
            if (!ThrownTools.onHit(this, result)) {
                super.onHit(result); // rebond sur les blocs géré par Spartan
            }
        }

        @Override
        public void onToolHit(EntityLivingBase victim) {
            arrowHit(victim); // EntityBoomerang : déclenche le retour
        }

        @Override
        protected boolean attemptCatch(EntityPlayer player) {
            return catchTool(this, player);
        }
    }
}
