package tconspartan.client;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transfère sur nos armes les rendus de modificateurs <b>dessinés à la main</b> par Tinkers, en les lisant sur
 * l'outil Tinkers le plus proche (épée pour les lames, hachette pour les haches, marteau...). Rien n'est copié :
 * tout est calculé au chargement des textures.
 *
 * <p>Deux cas, détectés automatiquement :</p>
 * <ul>
 *     <li><b>revêtement</b> (diamant, toucher de soie, décapitation, flamboyant...) : le dessin couvre une bonne
 *     partie de la tête. On apprend sa couleur par tranche de luminosité sur l'outil de référence, et on recolore la
 *     tête de l'arme dans la même zone (le long de l'axe) ;</li>
 *     <li><b>décorations</b> (gemme de hâte, émeraude, lapis de chance, anneau de châtiment, toiles...) : chaque
 *     groupe de pixels est déplacé au même endroit relatif de la pièce correspondante (manche, tête, garde).</li>
 * </ul>
 * <p>Repère : t = x - y (le long de l'arme), u = x + y - (taille - 1) (écart à l'axe).</p>
 */
@SideOnly(Side.CLIENT)
final class OverlayTransfer {

    static final int HANDLE = 1;
    static final int HEAD = 2;
    static final int EXTRA = 3;

    /** Outils Tinkers de référence : rôle de chacun de leurs calques (par nom de texture). */
    static final Map<String, Map<String, Integer>> REFERENCES = ImmutableMap.<String, Map<String, Integer>>builder()
            .put("broadsword", ImmutableMap.of("handle", HANDLE, "blade", HEAD, "guard", EXTRA))
            .put("rapier", ImmutableMap.of("handle", HANDLE, "blade", HEAD, "guard", EXTRA))
            .put("hatchet", ImmutableMap.of("handle", HANDLE, "head", HEAD, "binding", EXTRA))
            .put("pickaxe", ImmutableMap.of("handle", HANDLE, "head", HEAD, "binding", EXTRA))
            .put("hammer", ImmutableMap.of("handle", HANDLE, "head", HEAD, "back", HEAD, "front", HEAD))
            .put("scythe", ImmutableMap.of("handle", HANDLE, "head", HEAD, "binding", EXTRA, "accessory", EXTRA))
            .put("kama", ImmutableMap.of("handle", HANDLE, "head", HEAD, "binding", EXTRA))
            .build();

    private OverlayTransfer() {
    }

    // ------------------------------------------------------------------ source : outil Tinkers + dessin

    /** Calques d'un outil Tinkers de référence et dessin d'un modificateur pour cet outil. */
    static final class Source {
        final ResourceLocation overlay;
        final List<ResourceLocation> layers = new ArrayList<>();
        final List<Integer> roles = new ArrayList<>();

        Source(ResourceLocation overlay) {
            this.overlay = overlay;
        }

        List<ResourceLocation> textures() {
            List<ResourceLocation> all = new ArrayList<>(layers);
            all.add(overlay);
            return all;
        }
    }

    private static final Map<String, Source> CACHE = new HashMap<>();

    static void clearCache() {
        CACHE.clear();
    }

    /**
     * Trouve le dessin du modificateur : d'abord sur l'outil préféré, sinon sur un autre outil de référence
     * (ex. « explosif » n'existe que pour les outils de minage).
     *
     * @return null si Tinkers n'a aucun dessin utilisable pour ce modificateur.
     */
    static Source resolve(String preferredTool, String modifier) {
        String key = preferredTool + "/" + modifier;
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        Source source = null;
        JsonObject textures = readTextures("models/item/modifiers/" + modifier + ".json");
        if (textures != null) {
            List<String> candidates = new ArrayList<>();
            candidates.add(preferredTool);
            candidates.addAll(REFERENCES.keySet());
            for (String tool : candidates) {
                if (textures.has(tool) && REFERENCES.containsKey(tool)) {
                    source = toolSource(tool, new ResourceLocation(textures.get(tool).getAsString()));
                    if (source != null) {
                        break;
                    }
                }
            }
        }
        CACHE.put(key, source);
        return source;
    }

    private static Source toolSource(String tool, ResourceLocation overlay) {
        JsonObject textures = readTextures("models/item/tools/" + tool + ".tcon.json");
        if (textures == null) {
            return null;
        }
        Source source = new Source(overlay);
        Map<String, Integer> roles = REFERENCES.get(tool);
        for (int i = 0; textures.has("layer" + i); i++) {
            String path = textures.get("layer" + i).getAsString();
            String name = path.substring(path.lastIndexOf('/') + 1);
            source.layers.add(new ResourceLocation(path));
            source.roles.add(roles.getOrDefault(name, HEAD));
        }
        return source.layers.isEmpty() ? null : source;
    }

    private static JsonObject readTextures(String path) {
        try {
            IResource resource = Minecraft.getMinecraft().getResourceManager()
                    .getResource(new ResourceLocation("tconstruct", path));
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                return new JsonParser().parse(reader).getAsJsonObject().getAsJsonObject("textures");
            }
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ géométrie

    /** Rôle (manche/tête/garde) et luminosité de chaque pixel d'un outil. */
    static final class Geo {
        final int size;
        final int[] role;
        final int[] lum;

        Geo(int size, int[] role, int[] lum) {
            this.size = size;
            this.role = role;
            this.lum = lum;
        }

        /** Géométrie d'un outil Tinkers à partir de ses calques (le dernier calque l'emporte). */
        static Geo ofLayers(int size, List<int[]> layers, List<Integer> roles) {
            int[] role = new int[size * size];
            int[] lum = new int[size * size];
            for (int l = 0; l < layers.size(); l++) {
                int[] px = layers.get(l);
                for (int i = 0; i < role.length; i++) {
                    if (px[i] >>> 24 > 0) {
                        role[i] = roles.get(l);
                        lum[i] = SpartanTextures.luminance(px[i]);
                    }
                }
            }
            return new Geo(size, role, lum);
        }
    }

    /** Repère d'une pièce : plage le long de l'axe et ligne centrale u(t). */
    private static final class Frame {
        int tMin = Integer.MAX_VALUE;
        int tMax = Integer.MIN_VALUE;
        final Map<Integer, double[]> centre = new HashMap<>();

        void add(int t, int u) {
            tMin = Math.min(tMin, t);
            tMax = Math.max(tMax, t);
            double[] acc = centre.computeIfAbsent(t, k -> new double[2]);
            acc[0] += u;
            acc[1]++;
        }

        boolean empty() {
            return centre.isEmpty();
        }

        double norm(double t) {
            return (t - tMin) / Math.max(1, tMax - tMin);
        }

        double centreAt(double t) {
            int key = (int) Math.round(t);
            double[] acc = centre.get(key);
            if (acc == null) {
                int nearest = centre.keySet().stream().min(Comparator.comparingInt(c -> Math.abs(c - key))).orElse(key);
                acc = centre.get(nearest);
            }
            return acc[0] / acc[1];
        }
    }

    private static Frame frame(Geo g, int role) {
        Frame f = new Frame();
        for (int i = 0; i < g.role.length; i++) {
            if (g.role[i] == role) {
                int x = i % g.size;
                int y = i / g.size;
                f.add(x - y, x + y - (g.size - 1));
            }
        }
        return f;
    }

    // ------------------------------------------------------------------ transfert

    static int[] transfer(Geo ref, int[] overlay, Geo tgt) {
        int rw = ref.size;
        int tw = tgt.size;
        double scale = (double) tw / rw;
        int[] out = new int[tw * tw];
        boolean[] rest = new boolean[rw * rw];
        for (int i = 0; i < rest.length; i++) {
            rest[i] = overlay[i] >>> 24 > 0;
        }

        // 1) revêtement de la tête
        List<Integer> refHead = new ArrayList<>();
        List<Integer> covered = new ArrayList<>();
        for (int i = 0; i < ref.role.length; i++) {
            if (ref.role[i] == HEAD) {
                refHead.add(i);
                if (rest[i]) {
                    covered.add(i);
                }
            }
        }
        Frame rf = frame(ref, HEAD);
        Frame tf = frame(tgt, HEAD);
        if (!refHead.isEmpty() && !tf.empty() && covered.size() >= 0.3 * refHead.size()) {
            double a = Double.MAX_VALUE;
            double b = -Double.MAX_VALUE;
            for (int i : covered) {
                double s = rf.norm(i % rw - i / rw);
                a = Math.min(a, s);
                b = Math.max(b, s);
            }
            a -= 0.05;
            b += 0.05;
            // couleurs du dessin par tranche de luminosité de l'outil de référence (0 = pixel non couvert)
            Map<Integer, List<Integer>> bins = new HashMap<>();
            for (int i : refHead) {
                double s = rf.norm(i % rw - i / rw);
                if (s >= a && s <= b) {
                    bins.computeIfAbsent(ref.lum[i] / 32, k -> new ArrayList<>()).add(rest[i] ? overlay[i] : 0);
                }
            }
            int[] coat = new int[out.length];
            int zone = 0;
            int painted = 0;
            for (int i = 0; i < tgt.role.length; i++) {
                if (tgt.role[i] != HEAD) {
                    continue;
                }
                double s = tf.norm(i % tw - i / tw);
                if (s < a || s > b) {
                    continue;
                }
                zone++;
                int bin = tgt.lum[i] / 32;
                if (!bins.containsKey(bin)) {
                    final int wanted = bin;
                    bin = bins.keySet().stream().min(Comparator.comparingInt(k -> Math.abs(k - wanted))).orElse(bin);
                }
                List<Integer> candidates = bins.get(bin);
                List<Integer> hits = new ArrayList<>();
                for (int c : candidates) {
                    if (c != 0) {
                        hits.add(c);
                    }
                }
                if (!hits.isEmpty() && hits.size() * 2 >= candidates.size()) {
                    hits.sort(Comparator.comparingInt(SpartanTextures::luminance));
                    coat[i] = hits.get(hits.size() / 2);
                    painted++;
                }
            }
            // Si presque rien n'a été peint (luminosités trop différentes), on garde le mode décorations.
            if (zone > 0 && painted >= 0.25 * zone) {
                for (int i = 0; i < out.length; i++) {
                    if (coat[i] != 0) {
                        out[i] = coat[i];
                    }
                }
                for (int i : covered) {
                    rest[i] = false;
                }
            }
        }

        // 2) décorations : chaque groupe de pixels va au même endroit relatif de la pièce correspondante
        Map<Integer, Frame> refFrames = new LinkedHashMap<>();
        Map<Integer, Frame> tgtFrames = new LinkedHashMap<>();
        for (int role = HANDLE; role <= EXTRA; role++) {
            Frame r = frame(ref, role);
            Frame t = frame(tgt, role);
            if (!r.empty()) {
                refFrames.put(role, r);
            }
            if (!t.empty()) {
                tgtFrames.put(role, t);
            }
        }
        if (!tgtFrames.containsKey(HEAD)) {
            return out;
        }
        boolean[] seen = new boolean[rest.length];
        for (int start = 0; start < rest.length; start++) {
            if (!rest[start] || seen[start]) {
                continue;
            }
            List<Integer> group = new ArrayList<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(start);
            seen[start] = true;
            while (!stack.isEmpty()) {
                int p = stack.pop();
                group.add(p);
                int px = p % rw;
                int py = p / rw;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int qx = px + dx;
                        int qy = py + dy;
                        if (qx >= 0 && qy >= 0 && qx < rw && qy < rw) {
                            int q = qy * rw + qx;
                            if (rest[q] && !seen[q]) {
                                seen[q] = true;
                                stack.push(q);
                            }
                        }
                    }
                }
            }

            double cx = 0;
            double cy = 0;
            int[] votes = new int[4];
            for (int p : group) {
                cx += p % rw;
                cy += p / rw;
                votes[ref.role[p]]++;
            }
            cx /= group.size();
            cy /= group.size();
            int role = votes[HANDLE] + votes[HEAD] + votes[EXTRA] > 0 ? argmax(votes) : nearestRole(ref, cx, cy);
            Frame rfr = refFrames.get(role);
            if (rfr == null) {
                continue;
            }
            Frame tfr = tgtFrames.getOrDefault(role, tgtFrames.get(HEAD));

            double t = cx - cy;
            double u = cx + cy - (rw - 1);
            double s = rfr.norm(t);
            double du = u - rfr.centreAt(t);
            double tt = tfr.tMin + s * (tfr.tMax - tfr.tMin);
            double tu = tfr.centreAt(tt) + du * scale;
            double ax = (tt + tu + (tw - 1)) / 2;
            double ay = (tu + (tw - 1) - tt) / 2;
            int k = Math.max(1, (int) Math.round(scale));
            for (int p : group) {
                for (int i = 0; i < k; i++) {
                    for (int j = 0; j < k; j++) {
                        int x = (int) Math.round(ax + (p % rw - cx) * k) + i;
                        int y = (int) Math.round(ay + (p / rw - cy) * k) + j;
                        if (x >= 0 && y >= 0 && x < tw && y < tw) {
                            out[y * tw + x] = overlay[p];
                        }
                    }
                }
            }
        }
        return out;
    }

    private static int argmax(int[] votes) {
        int best = HANDLE;
        for (int r = HANDLE; r <= EXTRA; r++) {
            if (votes[r] > votes[best]) {
                best = r;
            }
        }
        return best;
    }

    private static int nearestRole(Geo ref, double cx, double cy) {
        int best = HEAD;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < ref.role.length; i++) {
            if (ref.role[i] != 0) {
                double dx = i % ref.size - cx;
                double dy = i / ref.size - cy;
                double d = dx * dx + dy * dy;
                if (d < bestDist) {
                    bestDist = d;
                    best = ref.role[i];
                }
            }
        }
        return best;
    }
}
