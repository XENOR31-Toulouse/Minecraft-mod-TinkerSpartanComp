package tconspartan.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.modifiers.IModifierDisplay;
import tconspartan.TConSpartan;
import tconspartan.tools.SpartanWeapon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Textures des outils construites à la volée à partir des calques "custom" de Spartan Weaponry
 * (aucune image de Spartan n'est copiée dans cet addon).
 *
 * <p>Spartan fournit pour chaque arme : layer_0 = manche (coloré), layer_1 = lame/tête (gris), layer_2 = reflets.
 * Tinkers colore une texture grise selon le matériau de chaque pièce. On découpe donc les calques en :</p>
 * <ul>
 *     <li>{@code handle} : le manche, converti en niveaux de gris ;</li>
 *     <li>{@code head} : la tête (layer_1 + reflets) ;</li>
 *     <li>{@code extra} : garde/attache/pommeau, pixels choisis par les paramètres de découpe de {@code WeaponDef} ;</li>
 *     <li>{@code mod_<modificateur>} : incrustations colorées sur la tête pour les modificateurs.</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class SpartanTextures {

    /** Modificateurs ayant un rendu sur les outils Tinkers (fichiers models/item/modifiers/*.json). */
    public static final List<String> MODIFIERS = Collections.unmodifiableList(Arrays.asList(
            "reinforced", "smite", "diamond", "blasting", "luck", "shulking", "bane_of_arthopods", "knockback",
            "beheading", "emerald", "silktouch", "mending_moss", "necrotic", "haste", "soulbound", "fiery",
            "sharpness", "glowing", "fortify", "webbed", "fins"));

    private static final int NONE = 0;
    private static final int HANDLE = 1;
    private static final int HEAD = 2;
    private static final int EXTRA = 3;

    private SpartanTextures() {
    }

    public static void register(TextureMap map) {
        for (SpartanWeapon weapon : SpartanWeapon.values()) {
            Sources sources = new Sources(weapon);
            map.setTextureEntry(new WeaponSprite(sprite(weapon, "handle"), weapon, sources, HANDLE, -1, 0));
            map.setTextureEntry(new WeaponSprite(sprite(weapon, "head"), weapon, sources, HEAD, -1, 0));
            if (weapon.hasExtraPart()) {
                map.setTextureEntry(new WeaponSprite(sprite(weapon, "extra"), weapon, sources, EXTRA, -1, 0));
            }
            for (int i = 0; i < MODIFIERS.size(); i++) {
                String modifier = MODIFIERS.get(i);
                map.setTextureEntry(new WeaponSprite(sprite(weapon, "mod_" + modifier), weapon, sources, NONE, i,
                        modifierColor(modifier)));
            }
        }
    }

    private static String sprite(SpartanWeapon weapon, String layer) {
        return TConSpartan.MODID + ":items/" + weapon.id() + "/" + layer;
    }

    private static int modifierColor(String id) {
        IModifier modifier = TinkerRegistry.getModifier(id);
        if (modifier instanceof IModifierDisplay) {
            return ((IModifierDisplay) modifier).getColor() & 0xFFFFFF;
        }
        return 0xDDDDDD; // ex. "fortify" : Tinkers le recolore selon le matériau
    }

    /** Emplacements des calques Spartan d'une arme (layer_0 et layer_2 peuvent manquer). */
    private static final class Sources {
        final ResourceLocation handle;
        final ResourceLocation head;
        final ResourceLocation shine;

        Sources(SpartanWeapon weapon) {
            handle = existing(weapon, 0);
            head = new ResourceLocation("spartanweaponry", "items/custom/" + weapon.spartanName() + "_layer_1");
            shine = existing(weapon, 2);
        }

        private static ResourceLocation existing(SpartanWeapon weapon, int layer) {
            String path = "items/custom/" + weapon.spartanName() + "_layer_" + layer;
            try {
                Minecraft.getMinecraft().getResourceManager()
                        .getResource(new ResourceLocation("spartanweaponry", "textures/" + path + ".png"));
                return new ResourceLocation("spartanweaponry", path);
            } catch (IOException e) {
                return null;
            }
        }

        List<ResourceLocation> all() {
            List<ResourceLocation> list = new ArrayList<>();
            list.add(head);
            if (handle != null) {
                list.add(handle);
            }
            if (shine != null) {
                list.add(shine);
            }
            return list;
        }
    }

    /** Sprite calculé à partir des calques Spartan (chargés avant lui grâce aux dépendances). */
    private static final class WeaponSprite extends TextureAtlasSprite {
        private final SpartanWeapon weapon;
        private final Sources sources;
        private final int part;
        private final int modifierIndex;
        private final int modifierColor;

        WeaponSprite(String name, SpartanWeapon weapon, Sources sources, int part, int modifierIndex, int modifierColor) {
            super(name);
            this.weapon = weapon;
            this.sources = sources;
            this.part = part;
            this.modifierIndex = modifierIndex;
            this.modifierColor = modifierColor;
        }

        @Override
        public Collection<ResourceLocation> getDependencies() {
            return sources.all();
        }

        @Override
        public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
            return true;
        }

        @Override
        public boolean load(IResourceManager manager, ResourceLocation location,
                            Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
            TextureAtlasSprite head = textureGetter.apply(sources.head);
            if (head == null || head.getFrameCount() == 0) {
                TConSpartan.LOG.error("Texture Spartan introuvable : {}", sources.head);
                return true; // échec : texture "missing"
            }
            int size = head.getIconWidth();
            Geometry geo = new Geometry(weapon, size, pixels(head, size),
                    pixels(get(textureGetter, sources.handle), size), pixels(get(textureGetter, sources.shine), size));

            int[] out = modifierIndex >= 0 ? geo.modifierOverlay(modifierIndex, modifierColor) : geo.layer(part);

            copyFrom(head);
            // Tableau couvrant tous les niveaux de mipmap (seul le niveau 0 est rempli, comme Tinkers).
            int[][] levels = new int[head.getFrameTextureData(0).length][];
            levels[0] = out;
            List<int[][]> frames = new ArrayList<>();
            frames.add(levels);
            setFramesTextureData(frames);
            return false;
        }

        private static TextureAtlasSprite get(Function<ResourceLocation, TextureAtlasSprite> getter, ResourceLocation rl) {
            return rl == null ? null : getter.apply(rl);
        }

        /** Pixels ARGB du sprite, ou null s'il manque ou n'a pas la bonne taille. */
        private static int[] pixels(TextureAtlasSprite sprite, int size) {
            if (sprite == null || sprite.getFrameCount() == 0 || sprite.getIconWidth() != size || sprite.getIconHeight() != size) {
                return null;
            }
            return sprite.getFrameTextureData(0)[0];
        }
    }

    /**
     * Attribue chaque pixel de l'arme à une pièce. Repère : t = x - y (le long de l'arme, du pommeau vers la
     * pointe), u = x + y - (taille - 1) (écart à l'axe ; 0 sur la diagonale du manche).
     */
    private static final class Geometry {
        final int size;
        final int[] composite;
        final int[] role;
        final boolean[] fromHandleLayer;

        Geometry(SpartanWeapon w, int size, int[] head, int[] handle, int[] shine) {
            this.size = size;
            int n = size * size;
            composite = new int[n];
            role = new int[n];
            fromHandleLayer = new boolean[n];

            int[] headShine = head.clone();
            if (shine != null) {
                blendOver(headShine, shine);
            }

            for (int i = 0; i < n; i++) {
                int x = i % size;
                int y = i / size;
                int t = x - y;
                int u = x + y - (size - 1);
                if (alpha(head[i]) > 0) {
                    composite[i] = headShine[i];
                    if (w.handleT() != null && t <= w.handleT()) {
                        role[i] = HANDLE;
                    } else if (w.hasExtraPart() && w.guardT() != null
                            && t - w.guardK() * Math.abs(u) <= w.guardT() && Math.abs(u) <= w.guardU()) {
                        role[i] = EXTRA;
                    } else {
                        role[i] = HEAD;
                    }
                } else if (handle != null && alpha(handle[i]) > 0) {
                    composite[i] = handle[i];
                    fromHandleLayer[i] = true;
                    role[i] = w.hasExtraPart() && Math.abs(u) > w.plateU() ? EXTRA : HANDLE;
                }
            }
            greyscaleHandleLayer();
        }

        /** Le calque manche de Spartan est coloré : on le passe en gris, contraste étiré comme les textures Tinkers. */
        private void greyscaleHandleLayer() {
            int min = 255;
            int max = 0;
            int[] lum = new int[composite.length];
            for (int i = 0; i < composite.length; i++) {
                if (fromHandleLayer[i]) {
                    lum[i] = luminance(composite[i]);
                    min = Math.min(min, lum[i]);
                    max = Math.max(max, lum[i]);
                }
            }
            for (int i = 0; i < composite.length; i++) {
                if (fromHandleLayer[i]) {
                    int v = max > min ? 60 + (lum[i] - min) * 160 / (max - min) : 150;
                    composite[i] = (alpha(composite[i]) << 24) | (v << 16) | (v << 8) | v;
                }
            }
        }

        int[] layer(int wanted) {
            int[] out = new int[composite.length];
            for (int i = 0; i < out.length; i++) {
                if (role[i] == wanted) {
                    out[i] = composite[i];
                }
            }
            return out;
        }

        /**
         * Incrustation d'un modificateur : un point de sa couleur sur la tête, à une position propre à chaque
         * modificateur (répartis du talon à la pointe) pour qu'ils restent visibles ensemble.
         */
        int[] modifierOverlay(int index, int rgb) {
            List<Integer> spots = new ArrayList<>();
            for (int i = 0; i < role.length; i++) {
                if (role[i] == HEAD && luminance(composite[i]) >= 90) {
                    spots.add(i);
                }
            }
            if (spots.isEmpty()) {
                for (int i = 0; i < role.length; i++) {
                    if (role[i] == HEAD) {
                        spots.add(i);
                    }
                }
            }
            int[] out = new int[role.length];
            if (spots.isEmpty()) {
                return out;
            }
            spots.sort(Comparator.<Integer>comparingInt(i -> (i % size) - (i / size)).thenComparingInt(i -> i));

            int anchor = spots.get((int) ((index + 0.5) * spots.size() / MODIFIERS.size()));
            int dot = Math.max(1, size / 16);
            int ax = anchor % size;
            int ay = anchor / size;
            int light = brighten(rgb);
            for (int dy = 0; dy < dot; dy++) {
                for (int dx = 0; dx < dot; dx++) {
                    paint(out, ax + dx, ay - dy, rgb);
                    paint(out, ax + dot + dx, ay - dot - dy, light); // reflet vers la pointe
                }
            }
            return out;
        }

        private void paint(int[] out, int x, int y, int rgb) {
            if (x < 0 || y < 0 || x >= size || y >= size) {
                return;
            }
            int i = y * size + x;
            if (role[i] == HEAD) {
                out[i] = 0xFF000000 | rgb;
            }
        }

        private static int brighten(int rgb) {
            int r = Math.min(255, ((rgb >> 16) & 0xFF) + 60);
            int g = Math.min(255, ((rgb >> 8) & 0xFF) + 60);
            int b = Math.min(255, (rgb & 0xFF) + 60);
            return (r << 16) | (g << 8) | b;
        }
    }

    private static int alpha(int argb) {
        return argb >>> 24;
    }

    /** Luminosité perçue, même formule que Tinkers (AbstractColoredTexture). */
    private static int luminance(int argb) {
        double r = ((argb >> 16) & 0xFF) / 255.0;
        double g = ((argb >> 8) & 0xFF) / 255.0;
        double b = (argb & 0xFF) / 255.0;
        return (int) (Math.sqrt(0.241 * r * r + 0.691 * g * g + 0.068 * b * b) * 255);
    }

    /** Superposition alpha "source over" (format ARGB). */
    private static void blendOver(int[] dst, int[] src) {
        for (int i = 0; i < dst.length && i < src.length; i++) {
            int sa = src[i] >>> 24;
            if (sa == 0) {
                continue;
            }
            int da = dst[i] >>> 24;
            float a = sa / 255f;
            int outA = Math.min(255, sa + (int) (da * (1 - a)));
            int r = Math.round(((src[i] >> 16) & 0xFF) * a + ((dst[i] >> 16) & 0xFF) * (1 - a));
            int g = Math.round(((src[i] >> 8) & 0xFF) * a + ((dst[i] >> 8) & 0xFF) * (1 - a));
            int b = Math.round((src[i] & 0xFF) * a + (dst[i] & 0xFF) * (1 - a));
            dst[i] = (outA << 24) | (r << 16) | (g << 8) | b;
        }
    }
}
