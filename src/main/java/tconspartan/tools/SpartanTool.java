package tconspartan.tools;

import com.google.common.collect.ImmutableSet;
import com.oblivioussp.spartanweaponry.api.IWeaponPropertyContainer;
import com.oblivioussp.spartanweaponry.api.ToolMaterialEx;
import com.oblivioussp.spartanweaponry.api.WeaponProperties;
import com.oblivioussp.spartanweaponry.api.weaponproperty.IPropertyCallback;
import com.oblivioussp.spartanweaponry.api.weaponproperty.WeaponProperty;
import com.oblivioussp.spartanweaponry.entity.projectile.EntityBoomerang;
import com.oblivioussp.spartanweaponry.entity.projectile.EntityThrownWeapon;
import com.oblivioussp.spartanweaponry.init.SoundRegistry;
import com.oblivioussp.spartanweaponry.item.IBlockingWeapon;
import com.oblivioussp.spartanweaponry.util.EntityDamageSourceArmorPiercing;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import slimeknights.tconstruct.library.Util;
import slimeknights.tconstruct.library.tinkering.Category;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import slimeknights.tconstruct.library.tools.SwordCore;
import slimeknights.tconstruct.library.tools.ToolNBT;
import slimeknights.tconstruct.library.utils.ToolHelper;
import tconspartan.throwing.Ammo;
import tconspartan.throwing.ThrowType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Un outil Tinkers (pièces, matériaux, traits, modificateurs) qui est aussi une arme Spartan.
 *
 * <p>En implémentant {@link IWeaponPropertyContainer}, Spartan Weaponry applique lui-même la portée étendue
 * (reach) et les bonus/malus de dégâts de mêlée (two-handed, backstab, extra_damage_*, damage_absorb). En
 * implémentant {@link IBlockingWeapon}, sa parade (dague de parade). Le reste est refait ici : balayage,
 * perforation d'armure, brise-bouclier, callbacks (fatigue des armes à deux mains, nausée...) et le lancer.</p>
 */
public class SpartanTool extends SwordCore implements IWeaponPropertyContainer<SpartanTool>, IBlockingWeapon {

    /**
     * Spartan exige un {@link ToolMaterialEx} ; il n'a pas de sens pour un outil Tinkers (le matériau dépend de
     * l'ItemStack), donc on fournit un matériau neutre sans aucune propriété.
     */
    static final ToolMaterialEx NEUTRAL_MATERIAL =
            new ToolMaterialEx("tinkers", Item.ToolMaterial.IRON, "ingotIron", "tconspartan", 0f);

    private static final ImmutableSet<Material> AXE_MATERIALS =
            ImmutableSet.of(Material.WOOD, Material.VINE, Material.PLANTS, Material.GOURD, Material.CACTUS);

    private final SpartanWeapon weapon;
    private final List<WeaponProperty> properties;

    public SpartanTool(SpartanWeapon weapon) {
        super(componentsOf(weapon));
        this.weapon = weapon;
        this.properties = new ArrayList<>(Arrays.asList(weapon.properties()));
        addCategory(Category.WEAPON);
        if (weapon.def.versatile) {
            setHarvestLevel("axe", 0); // niveau réel = stat de la tête, géré par Tinkers
        }
    }

    private static PartMaterialType[] componentsOf(SpartanWeapon weapon) {
        List<WeaponDef.Part> parts = weapon.parts();
        PartMaterialType[] types = new PartMaterialType[parts.size()];
        for (int i = 0; i < types.length; i++) {
            WeaponDef.Part part = parts.get(i);
            switch (part.role) {
                case HANDLE:
                    types[i] = PartMaterialType.handle(part.part.get());
                    break;
                case HEAD:
                    types[i] = PartMaterialType.head(part.part.get());
                    break;
                default:
                    types[i] = PartMaterialType.extra(part.part.get());
                    break;
            }
        }
        return types;
    }

    public SpartanWeapon getWeapon() {
        return weapon;
    }

    // ------------------------------------------------------------------ stats Tinkers

    @Override
    public float damagePotential() {
        return weapon.def.damagePotential;
    }

    @Override
    public double attackSpeed() {
        return weapon.def.attackSpeed;
    }

    @Override
    public float damageCutoff() {
        return weapon.def.damageCutoff;
    }

    @Override
    public float getRepairModifierForPart(int index) {
        return weapon.def.durabilityModifier;
    }

    @Override
    protected ToolNBT buildTagData(List<slimeknights.tconstruct.library.materials.Material> materials) {
        ToolNBT data = buildDefaultTag(materials);
        data.attack += weapon.def.attackBonus;
        data.durability = Math.max(1, (int) (data.durability * weapon.def.durabilityModifier));
        return data;
    }

    @Override
    public boolean isEffective(IBlockState state) {
        return super.isEffective(state) || (weapon.def.versatile && AXE_MATERIALS.contains(state.getMaterial()));
    }

    @Override
    public float miningSpeedModifier() {
        return weapon.def.versatile ? 1f : super.miningSpeedModifier();
    }

    // ------------------------------------------------------------------ combat

    @Override
    public boolean dealDamage(ItemStack stack, EntityLivingBase attacker, Entity target, float damage) {
        WeaponProperty piercing = getFirstWeaponPropertyWithType(WeaponProperties.PROPERTY_TYPE_ARMOUR_PIERCING);
        boolean hit;
        if (piercing != null) {
            // Source de dégâts de Spartan : son mixin sur applyArmorCalculations ignore une partie de l'armure.
            String type = attacker instanceof EntityPlayer ? "player" : "mob";
            hit = target.attackEntityFrom(
                    new EntityDamageSourceArmorPiercing(type, attacker, piercing.getMagnitude() / 100f), damage);
        } else {
            hit = super.dealDamage(stack, attacker, target, damage);
        }

        if (hit && !ToolHelper.isBroken(stack)) {
            sweepAttack(stack, attacker, target, damage);
        }
        return hit;
    }

    /** Reprend la logique de balayage de Spartan (WeaponHelper) et de la Broadsword Tinkers. */
    private void sweepAttack(ItemStack stack, EntityLivingBase attacker, Entity target, float damage) {
        WeaponProperty sweep = getFirstWeaponPropertyWithType(WeaponProperties.PROPERTY_TYPE_WIDE_SWEEP);
        if (sweep == null) {
            sweep = getFirstWeaponPropertyWithType(WeaponProperties.PROPERTY_TYPE_SWEEP_DAMAGE);
        }
        if (sweep == null || !(attacker instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) attacker;

        // Mêmes conditions que le balayage vanilla : coup chargé, au sol, sans sprint ni coup critique.
        boolean charged = player.getCooledAttackStrength(0.5f) > 0.9f;
        boolean critical = player.fallDistance > 0f && !player.onGround && !player.isOnLadder() && !player.isInWater()
                && !player.isPotionActive(MobEffects.BLINDNESS) && !player.isRiding();
        double walked = player.distanceWalkedModified - player.prevDistanceWalkedModified;
        if (!charged || critical || player.isSprinting() || !player.onGround || walked >= player.getAIMoveSpeed()) {
            return;
        }

        // Niveau 1 = balayage vanilla (1 dégât + Sweeping Edge), niveaux 2/3 = 50 % / 100 % des dégâts.
        float sweepDamage = sweep.getMagnitude() <= 1f
                ? 1f + EnchantmentHelper.getSweepingDamageRatio(player) * damage
                : damage * sweep.getMagnitude() / 100f;

        AxisAlignedBB box = target.getEntityBoundingBox().grow(1.0, 0.25, 1.0);
        if (WeaponProperties.PROPERTY_TYPE_WIDE_SWEEP.equals(sweep.getType())) {
            box = box.grow(1.0);
        }
        double reach = getReach();
        float yawRad = player.rotationYaw * 0.017453292f;

        for (EntityLivingBase victim : player.world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (victim != player && victim != target && !player.isOnSameTeam(victim)
                    && player.getDistanceSq(victim) < reach * reach) {
                victim.knockBack(player, 0.4f, MathHelper.sin(yawRad), -MathHelper.cos(yawRad));
                super.dealDamage(stack, player, victim, sweepDamage);
            }
        }
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, player.getSoundCategory(), 1f, 1f);
        player.spawnSweepParticles();
    }

    private double getReach() {
        WeaponProperty reach = getFirstWeaponPropertyWithType(WeaponProperties.PROPERTY_TYPE_REACH);
        return reach != null ? reach.getMagnitude() : 3.0;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        for (WeaponProperty prop : properties) {
            IPropertyCallback callback = prop.getCallback();
            if (callback != null) {
                callback.onHitEntity(NEUTRAL_MATERIAL, stack, target, attacker, null);
            }
        }
        return super.hitEntity(stack, target, attacker);
    }

    /** Bonus Spartan d'une arme lancée (le handler de mêlée de Spartan ne s'applique pas aux projectiles). */
    public float applyThrownProperties(ItemStack stack, float damage, DamageSource source,
                                       EntityLivingBase attacker, EntityLivingBase victim, Entity projectile) {
        float initial = ToolHelper.getActualAttack(stack) + 1f;
        for (WeaponProperty prop : properties) {
            IPropertyCallback callback = prop.getCallback();
            if (callback != null) {
                damage = callback.modifyDamageDealt(NEUTRAL_MATERIAL, damage, initial, source, attacker, victim);
                callback.onHitEntity(NEUTRAL_MATERIAL, stack, victim, attacker, projectile);
            }
        }
        return damage;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        super.onUpdate(stack, world, entity, itemSlot, isSelected);
        if (entity instanceof EntityLivingBase) {
            // Ex. "two-handed" : fatigue de minage si l'autre main est occupée.
            for (WeaponProperty prop : properties) {
                IPropertyCallback callback = prop.getCallback();
                if (callback != null) {
                    callback.onItemUpdate(NEUTRAL_MATERIAL, stack, world, (EntityLivingBase) entity, itemSlot, isSelected);
                }
            }
        }
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, EntityLivingBase entity, EntityLivingBase attacker) {
        return hasWeaponProperty(WeaponProperties.SHIELD_BREACH) || super.canDisableShield(stack, shield, entity, attacker);
    }

    // ------------------------------------------------------------------ clic droit : lancer / parer

    private boolean usable() {
        return weapon.throwType().canThrow() || weapon.def.blocking;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        if (weapon.def.blocking) {
            return EnumAction.BLOCK;
        }
        return weapon.throwType().canThrow() ? EnumAction.BOW : super.getItemUseAction(stack);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return usable() ? 72000 : super.getMaxItemUseDuration(stack);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (usable() && !ToolHelper.isBroken(stack)) {
            player.setActiveHand(hand);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return super.onItemRightClick(world, player, hand);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase living, int timeLeft) {
        ThrowType type = weapon.throwType();
        if (type.canThrow() && living instanceof EntityPlayer && !ToolHelper.isBroken(stack)) {
            throwWeapon(stack, world, (EntityPlayer) living, type, getMaxItemUseDuration(stack) - timeLeft);
        }
        super.onPlayerStoppedUsing(stack, world, living, timeLeft);
    }

    private void throwWeapon(ItemStack stack, World world, EntityPlayer player, ThrowType type, int usedTicks) {
        int charge = Math.min(usedTicks, type.chargeTicks);
        if (charge <= 2) {
            return; // simple clic : on ne lance pas
        }
        float chargePerc = (float) charge / type.chargeTicks;

        int ammo = Ammo.get(stack);
        if (!world.isRemote) {
            EntityThrownWeapon thrown = type.create(world, player);
            // Le projectile emporte une seule munition, liée à cette pile pour y revenir au ramassage.
            thrown.setWeapon(Ammo.splitForThrow(stack));
            thrown.shoot(player, player.rotationPitch, player.rotationYaw, 0f,
                    type.velocity * (chargePerc * 0.9f + 0.1f), 0.5f);
            float multiplier = (type.damageMultiplier - 1f) * chargePerc + 1f;
            thrown.setDamage((ToolHelper.getActualAttack(stack) + 1f) * multiplier);
            if (thrown instanceof EntityBoomerang) {
                ((EntityBoomerang) thrown).setMaxDistance(charge / 5.0 * 2.0 + 3.0);
            }
            if (player.capabilities.isCreativeMode) {
                thrown.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
            }
            world.spawnEntity(thrown);
            world.playSound(null, player.posX, player.posY, player.posZ,
                    type == ThrowType.BOOMERANG ? SoundRegistry.BOOMERANG_THROW : SoundRegistry.THROWING_WEAPON_THROW,
                    SoundCategory.PLAYERS, 0.5f, 0.4f / (itemRand.nextFloat() * 0.4f + 0.8f));
        }
        if (!player.capabilities.isCreativeMode) {
            if (ammo > 1) {
                Ammo.set(stack, ammo - 1);
            } else {
                player.inventory.deleteStack(stack);
            }
        }
        player.addStat(StatList.getObjectUseStats(this));
    }

    // ------------------------------------------------------------------ IBlockingWeapon (parade Spartan)

    @Override
    public boolean canBlockMelee() {
        return weapon.def.blocking;
    }

    @Override
    public boolean canBlockProjectiles() {
        return false;
    }

    // ------------------------------------------------------------------ tooltip

    /** Les armes de lancer en pile affichent leur nombre de munitions : "Couteau de lancer en fer ×12". */
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String name = super.getItemStackDisplayName(stack);
        return weapon.maxAmmo() > 1 ? name + " ×" + Ammo.get(stack) : name;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getTooltip(ItemStack stack, List<String> tooltips) {
        super.getTooltip(stack, tooltips);
        if (weapon.maxAmmo() > 1) {
            tooltips.add(I18n.format("tooltip.tconspartan.ammo", Ammo.get(stack), weapon.maxAmmo()));
        }
        if (!properties.isEmpty()) {
            boolean shift = Util.isShiftKeyDown();
            tooltips.add(TextFormatting.GOLD + I18n.format("tooltip.tconspartan.properties"));
            for (WeaponProperty prop : properties) {
                prop.addTooltip(stack, tooltips, shift);
            }
        }
    }

    // ------------------------------------------------------------------ IWeaponPropertyContainer

    @Override
    public boolean hasWeaponProperty(WeaponProperty prop) {
        return properties.contains(prop);
    }

    @Override
    public SpartanTool addWeaponProperty(WeaponProperty prop) {
        properties.add(prop);
        return this;
    }

    @Override
    public WeaponProperty getFirstWeaponPropertyWithType(String type) {
        for (WeaponProperty prop : properties) {
            if (prop.getType().equals(type)) {
                return prop;
            }
        }
        return null;
    }

    @Override
    public List<WeaponProperty> getAllWeaponPropertiesWithType(String type) {
        List<WeaponProperty> result = new ArrayList<>();
        for (WeaponProperty prop : properties) {
            if (prop.getType().equals(type)) {
                result.add(prop);
            }
        }
        // Même contrat que Spartan : null si aucune propriété.
        return result.isEmpty() ? null : result;
    }

    @Override
    public List<WeaponProperty> getAllWeaponProperties() {
        return new ArrayList<>(properties);
    }

    @Override
    public ToolMaterialEx getMaterialEx() {
        return NEUTRAL_MATERIAL;
    }

    /**
     * Spartan plafonne ses bonus de dégâts sur "dégâts directs de l'arme + 1". Pour un outil Tinkers cette
     * valeur dépend de l'ItemStack : elle est fournie par {@link AttackContext} pendant le LivingHurtEvent.
     */
    @Override
    public float getDirectAttackDamage() {
        return AttackContext.currentAttackDamage();
    }
}
