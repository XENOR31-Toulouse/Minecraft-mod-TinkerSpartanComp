package tconspartan.tools;

import com.oblivioussp.spartanweaponry.api.weaponproperty.WeaponProperty;
import slimeknights.tconstruct.library.tools.ToolPart;
import tconspartan.throwing.ThrowType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Description d'une arme (pièces, stats, propriétés, découpe de texture). Voir {@link SpartanWeapon}. */
public final class WeaponDef {

    /** Rôle d'une pièce : détermine quelles stats du matériau sont utilisées. */
    public enum Role { HANDLE, HEAD, EXTRA }

    public static final class Part {
        public final Role role;
        public final Supplier<ToolPart> part;

        Part(Role role, Supplier<ToolPart> part) {
            this.role = role;
            this.part = part;
        }
    }

    final String spartanName;
    final List<Part> parts = new ArrayList<>();
    boolean forgeOnly;
    float damagePotential = 1f;
    double attackSpeed = 1.6;
    float attackBonus = 1f;
    float durabilityModifier = 1f;
    float damageCutoff = 15f;
    ThrowType throwType = ThrowType.NONE;
    int durabilityPerAmmo;
    boolean blocking;
    boolean versatile;
    WeaponProperty[] properties = new WeaponProperty[0];

    // Découpe des textures Spartan (voir SpartanTextures). t = x - y (le long de l'arme), u = x + y - (taille - 1).
    Integer guardT;
    int guardU = 99;
    int guardK = 1;
    int plateU = 99;
    Integer handleT;

    private WeaponDef(String spartanName) {
        this.spartanName = spartanName;
    }

    /** @param spartanName nom de l'arme dans Spartan Weaponry (sert à trouver ses textures). */
    static WeaponDef of(String spartanName) {
        return new WeaponDef(spartanName);
    }

    WeaponDef handle(Supplier<ToolPart> part) {
        parts.add(new Part(Role.HANDLE, part));
        return this;
    }

    WeaponDef head(Supplier<ToolPart> part) {
        parts.add(new Part(Role.HEAD, part));
        return this;
    }

    WeaponDef extra(Supplier<ToolPart> part) {
        parts.add(new Part(Role.EXTRA, part));
        return this;
    }

    /** Uniquement à la Tool Forge (grosses armes). */
    WeaponDef forge() {
        this.forgeOnly = true;
        return this;
    }

    WeaponDef stats(float damagePotential, double attackSpeed, float durabilityModifier, float damageCutoff) {
        this.damagePotential = damagePotential;
        this.attackSpeed = attackSpeed;
        this.durabilityModifier = durabilityModifier;
        this.damageCutoff = damageCutoff;
        return this;
    }

    WeaponDef attackBonus(float bonus) {
        this.attackBonus = bonus;
        return this;
    }

    WeaponDef throwing(ThrowType type) {
        this.throwType = type;
        return this;
    }

    /**
     * Arme de lancer à munitions, comme les flèches Tinkers : nombre de projectiles = durabilité / cette valeur
     * (voir throwing.Ammo). La durabilité dépend des matériaux, donc le nombre de projectiles aussi.
     */
    WeaponDef ammo(int durabilityPerAmmo) {
        this.durabilityPerAmmo = durabilityPerAmmo;
        return this;
    }

    /** Parade au clic droit (propriété BLOCK_MELEE de Spartan). */
    WeaponDef blocking() {
        this.blocking = true;
        return this;
    }

    /** Sert aussi de hache (propriété VERSATILE de Spartan). */
    WeaponDef versatile() {
        this.versatile = true;
        return this;
    }

    WeaponDef props(WeaponProperty... properties) {
        this.properties = properties;
        return this;
    }

    /**
     * Pixels de la texture « tête » de Spartan attribués à la garde/attache (3e pièce) :
     * ceux où {@code t - k*|u| <= maxT} et {@code |u| <= maxU}.
     */
    WeaponDef guard(int maxT, int maxU, int k) {
        this.guardT = maxT;
        this.guardU = maxU;
        this.guardK = k;
        return this;
    }

    /** Pixels du manche Spartan éloignés de l'axe ({@code |u| > minU}) attribués à la 3e pièce (ex. garde de lance). */
    WeaponDef plate(int minU) {
        this.plateU = minU;
        return this;
    }

    /** Pour les armes sans calque de manche : pixels de la tête avec {@code t <= maxT} = manche. */
    WeaponDef handleBelow(int maxT) {
        this.handleT = maxT;
        return this;
    }
}
