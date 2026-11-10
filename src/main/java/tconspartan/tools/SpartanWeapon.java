package tconspartan.tools;

import com.oblivioussp.spartanweaponry.api.weaponproperty.WeaponProperty;
import slimeknights.tconstruct.library.tools.ToolPart;
import tconspartan.throwing.ThrowType;

import java.util.List;
import java.util.Locale;

import static com.oblivioussp.spartanweaponry.api.WeaponProperties.*;
import static tconspartan.tools.TinkerParts.*;

/**
 * Les armes Spartan converties en outils Tinkers, fabriquées uniquement avec des pièces existantes de Tinkers.
 *
 * <p>La Tool Station associe les pièces <b>dans l'ordre des emplacements</b> et prend le premier outil qui
 * correspond : chaque combinaison ci-dessous est donc unique (entre elles et vis-à-vis des outils de Tinkers).</p>
 *
 * <p>Stats : {@code damagePotential} = dégâts Spartan (fer, config par défaut) / épée vanilla ; la vitesse est
 * celle de Spartan. Les noms de registre évitent ceux des outils Tinkers (scythe, hammer, battleaxe) car Tinkers
 * indexe certains modèles par nom d'outil sans le domaine.</p>
 */
public enum SpartanWeapon {
    // ---------------------------------------------------------------- Tool Station
    DAGGER(WeaponDef.of("dagger").handle(ROD).head(KNIFE_BLADE).extra(CROSS_GUARD)
            .stats(0.55f, 2.5, 0.8f, 15f).attackBonus(0.5f).throwing(ThrowType.DAGGER)
            .props(THROWABLE, EXTRA_DAMAGE_BACKSTAB).guard(-2, 99, 1)),
    PARRYING_DAGGER(WeaponDef.of("parrying_dagger").handle(ROD).head(KNIFE_BLADE).extra(HAND_GUARD)
            .stats(0.45f, 2.5, 0.9f, 15f).attackBonus(0.5f).blocking()
            .props(BLOCK_MELEE).guard(-2, 99, 1).handleBelow(-8)),
    KATANA(WeaponDef.of("katana").handle(ROD).head(SWORD_BLADE).extra(BINDING)
            .stats(0.8f, 2.0, 1.0f, 15f)
            .props(TWO_HANDED_1, SWEEP_DAMAGE_NORMAL, EXTRA_DAMAGE_2_CHEST).guard(-4, 99, 1)),
    SABER(WeaponDef.of("saber").handle(ROD).head(SWORD_BLADE).extra(TOUGH_BINDING)
            .stats(0.9f, 1.6, 1.0f, 15f)
            .props(DAMAGE_ABSORB_25, EXTRA_DAMAGE_2_CHEST, SWEEP_DAMAGE_NORMAL).guard(-3, 99, 1)),
    SPEAR(WeaponDef.of("spear").handle(TOUGH_ROD).head(KNIFE_BLADE).extra(BINDING)
            .stats(0.9f, 1.4, 1.1f, 15f)
            .props(REACH_1).guard(19, 99, 1)),
    MACE(WeaponDef.of("mace").handle(ROD).head(HAMMER_HEAD).extra(BINDING)
            .stats(1.0f, 1.2, 1.1f, 16f)
            .props(EXTRA_DAMAGE_50P_UNDEAD).guard(2, 1, 0)),
    QUARTERSTAFF(WeaponDef.of("quarterstaff").handle(TOUGH_ROD).head(TOUGH_ROD).extra(BINDING)
            .stats(0.5f, 2.0, 1.2f, 15f)
            .props(TWO_HANDED_1, SWEEP_DAMAGE_HALF).guard(-10, 99, 0)),
    THROWING_KNIFE(WeaponDef.of("throwing_knife").handle(ROD).head(KNIFE_BLADE)
            .stats(0.3f, 2.5, 0.8f, 15f).attackBonus(0.5f).throwing(ThrowType.KNIFE).ammo(16)
            .props(THROWABLE, EXTRA_DAMAGE_2_THROWN)),
    THROWING_AXE(WeaponDef.of("throwing_axe").handle(ROD).head(AXE_HEAD)
            .stats(0.6f, 0.9, 0.9f, 15f).throwing(ThrowType.AXE).ammo(8)
            .props(THROWABLE, EXTRA_DAMAGE_2_THROWN)),
    JAVELIN(WeaponDef.of("javelin").handle(TOUGH_ROD).head(ARROW_HEAD)
            .stats(0.3f, 1.2, 1.0f, 15f).throwing(ThrowType.JAVELIN).ammo(4)
            .props(THROWABLE, EXTRA_DAMAGE_3_THROWN)),
    BOOMERANG(WeaponDef.of("boomerang").handle(ROD).head(KAMA_HEAD)
            .stats(1.0f, 1.4, 1.0f, 15f).throwing(ThrowType.BOOMERANG)
            .props(THROWABLE)),

    // ---------------------------------------------------------------- Tool Forge
    PIKE(WeaponDef.of("pike").forge().handle(TOUGH_ROD).head(SWORD_BLADE).extra(TOUGH_BINDING)
            .stats(0.8f, 1.4, 1.2f, 18f)
            .props(TWO_HANDED_1, REACH_2).guard(44, 99, 1)),
    LANCE(WeaponDef.of("lance").forge().handle(TOUGH_ROD).head(SWORD_BLADE).extra(LARGE_PLATE)
            .stats(1.0f, 1.0, 1.3f, 20f)
            .props(REACH_1, EXTRA_DAMAGE_2_RIDING).plate(1)),
    GLAIVE(WeaponDef.of("glaive").forge().handle(TOUGH_ROD).head(LARGE_SWORD_BLADE).extra(TOUGH_BINDING)
            .stats(1.1f, 1.0, 1.3f, 20f)
            .props(TWO_HANDED_1, REACH_1, SWEEP_DAMAGE_HALF).guard(8, 99, 1)),
    GREATSWORD(WeaponDef.of("greatsword").forge().handle(TOUGH_ROD).head(LARGE_SWORD_BLADE).extra(WIDE_GUARD)
            .stats(1.6f, 1.4, 1.3f, 22f)
            .props(TWO_HANDED_2, REACH_1, SWEEP_DAMAGE_FULL).guard(-12, 99, 1)),
    HALBERD(WeaponDef.of("halberd").forge().handle(TOUGH_ROD).head(AXE_HEAD).extra(TOUGH_BINDING)
            .stats(1.8f, 1.2, 1.4f, 24f)
            .props(TWO_HANDED_2, REACH_1, SHIELD_BREACH).guard(17, 2, 0)),
    BATTLE_AXE(WeaponDef.of("battleaxe").forge().handle(TOUGH_ROD).head(BROAD_AXE_HEAD).extra(TOUGH_BINDING)
            .stats(1.6f, 1.0, 1.3f, 22f).versatile()
            .props(TWO_HANDED_1, VERSATILE).guard(5, 1, 0)),
    WARHAMMER(WeaponDef.of("warhammer").forge().handle(TOUGH_ROD).head(PICK_HEAD).extra(TOUGH_BINDING)
            .stats(1.1f, 1.1, 1.3f, 20f)
            .props(TWO_HANDED_1, ARMOUR_PIERCING_50).guard(5, 1, 0)),
    BATTLE_HAMMER(WeaponDef.of("hammer").forge().handle(TOUGH_ROD).head(HAMMER_HEAD).extra(TOUGH_BINDING)
            .stats(1.4f, 0.8, 1.4f, 22f)
            .props(KNOCKBACK, NAUSEA).guard(3, 2, 0)),
    WAR_SCYTHE(WeaponDef.of("scythe").forge().handle(TOUGH_ROD).head(SCYTHE_HEAD).extra(BINDING)
            .stats(1.2f, 1.0, 1.3f, 20f)
            .props(TWO_HANDED_2, WIDE_SWEEP, EXTRA_DAMAGE_2_HEAD).guard(19, 3, 0));

    public final WeaponDef def;

    /** Rempli par {@code ModTools} à l'enregistrement des items. */
    public SpartanTool tool;

    SpartanWeapon(WeaponDef def) {
        this.def = def;
    }

    /** Nom de registre de l'outil, ex. "katana" ou "war_scythe". */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Nom de l'arme dans Spartan Weaponry, pour ses textures ("scythe", "hammer"...). */
    public String spartanName() {
        return def.spartanName;
    }

    public List<WeaponDef.Part> parts() {
        return def.parts;
    }

    /** Nombre de pièces = nombre de calques du modèle (manche, tête, [garde/attache]). */
    public boolean hasExtraPart() {
        return def.parts.size() > 2;
    }

    public boolean forgeOnly() {
        return def.forgeOnly;
    }

    public ThrowType throwType() {
        return def.throwType;
    }

    /** Projectiles par pile (1 = pas de munitions). */
    public int maxAmmo() {
        return def.maxAmmo;
    }

    public WeaponProperty[] properties() {
        return def.properties;
    }

    // Découpe des textures (voir SpartanTextures)
    public Integer guardT() { return def.guardT; }
    public int guardU() { return def.guardU; }
    public int guardK() { return def.guardK; }
    public int plateU() { return def.plateU; }
    public Integer handleT() { return def.handleT; }

    public ToolPart part(int index) {
        return def.parts.get(index).part.get();
    }
}
