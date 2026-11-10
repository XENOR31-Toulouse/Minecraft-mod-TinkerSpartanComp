package tconspartan.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import slimeknights.tconstruct.library.utils.TagUtil;
import tconspartan.TConSpartan;
import tconspartan.tools.SpartanWeapon;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Armes affichées en 3D en main (le caestus, gant enroulé autour du poing comme dans Spartan Weaponry).
 *
 * <p>Le modèle Tinkers (icône plate, avec les modificateurs) reste utilisé dans l'inventaire et les tables. En main
 * et au sol, on affiche le modèle 3D de Spartan, re-texturé avec les versions par matériau (générées par Tinkers) de
 * nos textures cuir/métal : le cuir prend le matériau de l'attache, le métal celui de la garde.</p>
 */
@SideOnly(Side.CLIENT)
public final class HeldModels {

    private HeldModels() {
    }

    public static void onModelBake(ModelBakeEvent event) {
        for (SpartanWeapon weapon : SpartanWeapon.values()) {
            if (weapon.heldModel() == null) {
                continue;
            }
            // Emplacement donné par ModelRegisterUtil.registerToolModel de Tinkers
            ModelResourceLocation location = new ModelResourceLocation(
                    new ResourceLocation(TConSpartan.MODID, "tools/" + weapon.id() + ".tcon"), "inventory");
            IBakedModel tinkers = event.getModelRegistry().getObject(location);
            if (tinkers == null) {
                TConSpartan.LOG.warn("Modèle Tinkers introuvable pour {}", location);
                continue;
            }
            IModel spartan = ModelLoaderRegistry.getModelOrMissing(new ResourceLocation("spartanweaponry", weapon.heldModel()));
            event.getModelRegistry().putObject(location, new Wrapper(tinkers, spartan, weapon));
        }
    }

    private static boolean held(ItemCameraTransforms.TransformType type) {
        switch (type) {
            case FIRST_PERSON_LEFT_HAND:
            case FIRST_PERSON_RIGHT_HAND:
            case THIRD_PERSON_LEFT_HAND:
            case THIRD_PERSON_RIGHT_HAND:
            case GROUND:
                return true;
            default:
                return false;
        }
    }

    /** Modèle enregistré pour l'objet : délègue à Tinkers, et choisit la vue 3D selon l'ItemStack. */
    private static final class Wrapper extends Delegating {
        private final IModel spartan;
        private final SpartanWeapon weapon;
        private final Map<String, IBakedModel> baked3d = new HashMap<>();
        private final ItemOverrideList overrides;

        Wrapper(IBakedModel tinkers, IModel spartan, SpartanWeapon weapon) {
            super(tinkers);
            this.spartan = spartan;
            this.weapon = weapon;
            this.overrides = new ItemOverrideList(Collections.emptyList()) {
                @Override
                public IBakedModel handleItemState(IBakedModel original, ItemStack stack, @Nullable World world,
                                                   @Nullable EntityLivingBase entity) {
                    IBakedModel flat = tinkers.getOverrides().handleItemState(tinkers, stack, world, entity);
                    IBakedModel model3d = model3d(stack);
                    return model3d == null ? flat : new PerStack(flat, model3d);
                }
            };
        }

        @Override
        public ItemOverrideList getOverrides() {
            return overrides;
        }

        /** Modèle 3D pour les matériaux de l'objet (manche = cuir, tête = métal), mis en cache. */
        @Nullable
        private IBakedModel model3d(ItemStack stack) {
            NBTTagList materials = TagUtil.getBaseMaterialsTagList(stack);
            String strapMaterial = materials.getStringTagAt(0);
            String metalMaterial = materials.getStringTagAt(1);
            String key = strapMaterial + "|" + metalMaterial;
            if (!baked3d.containsKey(key)) {
                IBakedModel strap = bake(SpartanTextures.heldTexture(weapon, false), strapMaterial);
                IBakedModel metal = bake(SpartanTextures.heldTexture(weapon, true), metalMaterial);
                baked3d.put(key, strap == null || metal == null ? null : new Combined(strap, metal));
            }
            return baked3d.get(key);
        }

        @Nullable
        private IBakedModel bake(String baseTexture, String material) {
            // Tinkers crée « <base>_<matériau> » dans l'atlas (CustomTextureCreator). Son index est vidé après le
            // chargement des textures, donc on cherche le sprite directement dans l'atlas.
            TextureMap atlas = Minecraft.getMinecraft().getTextureMapBlocks();
            TextureAtlasSprite sprite = atlas.getAtlasSprite(baseTexture + "_" + material);
            String texture = sprite != atlas.getMissingSprite() ? sprite.getIconName() : baseTexture;
            try {
                IModel retextured = spartan.retexture(ImmutableMap.of("0", texture, "particle", texture));
                return retextured.bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM,
                        rl -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(rl.toString()));
            } catch (Exception e) {
                TConSpartan.LOG.error("Impossible de construire le modèle 3D de {}", weapon.id(), e);
                return null;
            }
        }
    }

    /** Un objet précis : icône Tinkers dans l'inventaire, modèle 3D en main et au sol. */
    private static final class PerStack extends Delegating {
        private final IBakedModel model3d;

        PerStack(IBakedModel flat, IBakedModel model3d) {
            super(flat);
            this.model3d = model3d;
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
            return held(type) ? model3d.handlePerspective(type) : delegate.handlePerspective(type);
        }
    }

    /** Cuir puis métal, même géométrie : le métal recouvre le cuir là où il est opaque. */
    private static final class Combined extends Delegating {
        private final IBakedModel metal;

        Combined(IBakedModel strap, IBakedModel metal) {
            super(strap);
            this.metal = metal;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            List<BakedQuad> quads = new ArrayList<>(delegate.getQuads(state, side, rand));
            quads.addAll(metal.getQuads(state, side, rand));
            return ImmutableList.copyOf(quads);
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
            return Pair.of(this, delegate.handlePerspective(type).getRight());
        }
    }

    /** Délègue tout au modèle d'origine. */
    private abstract static class Delegating implements IBakedModel {
        protected final IBakedModel delegate;

        Delegating(IBakedModel delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            return delegate.getQuads(state, side, rand);
        }

        @Override
        public boolean isAmbientOcclusion() {
            return delegate.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return delegate.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return delegate.isBuiltInRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return delegate.getParticleTexture();
        }

        @Override
        public ItemOverrideList getOverrides() {
            return delegate.getOverrides();
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
            return delegate.handlePerspective(type);
        }
    }
}
