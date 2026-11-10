package tconspartan.throwing;

import com.oblivioussp.spartanweaponry.entity.projectile.EntityThrownWeapon;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import java.util.function.BiFunction;

/**
 * Manière de lancer une arme. Valeurs reprises de Spartan Weaponry (ItemThrowingWeapon et sous-classes).
 *
 * <p>Chaque type a sa propre entité (sous-classe des projectiles de Spartan) pour réutiliser leur rendu.</p>
 */
public enum ThrowType {
    /** Pas lançable. */
    NONE(0f, 1f, 0, null),
    /** Dague : lancer « de secours » (ItemSwordBase de Spartan). */
    DAGGER(1.5f, 1f, 5, ThrownTools.Dagger::new),
    KNIFE(2.1f, 2f, 5, ThrownTools.Knife::new),
    AXE(1.75f, 2f, 8, ThrownTools.Axe::new),
    JAVELIN(2.4f, 3f, 10, ThrownTools.Javelin::new),
    BOOMERANG(2f, 1.7f, 5, ThrownTools.Boomerang::new);

    /** Vitesse du projectile à pleine charge. */
    public final float velocity;
    /** Multiplicateur de dégâts à pleine charge (1 = dégâts de mêlée). */
    public final float damageMultiplier;
    /** Durée (ticks) pour charger complètement le lancer. */
    public final int chargeTicks;
    private final BiFunction<World, EntityLivingBase, EntityThrownWeapon> factory;

    ThrowType(float velocity, float damageMultiplier, int chargeTicks,
              BiFunction<World, EntityLivingBase, EntityThrownWeapon> factory) {
        this.velocity = velocity;
        this.damageMultiplier = damageMultiplier;
        this.chargeTicks = chargeTicks;
        this.factory = factory;
    }

    public boolean canThrow() {
        return factory != null;
    }

    public EntityThrownWeapon create(World world, EntityLivingBase thrower) {
        return factory.apply(world, thrower);
    }
}
