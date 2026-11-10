package tconspartan.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Art des modificateurs, dessiné à partir de la forme réelle de chaque arme.
 *
 * <p>Les armes Spartan n'ont ni la même résolution (16, 32 ou 64 px) ni la même silhouette : chaque motif est
 * donc posé sur un élément repéré dans la forme (fil du tranchant, gorge, pointe, garde, pommeau, poignée), avec
 * des règles communes :</p>
 * <ul>
 *     <li>on ne déborde jamais de la silhouette (sauf les éclats de lumière, voulus) ;</li>
 *     <li>chaque modificateur a sa place (tranchant, gorge, garde, pommeau, poignée, autour) : plusieurs se
 *     combinent sans se chevaucher ;</li>
 *     <li>palettes de 4 tons choisies à la main, et ombrage selon la luminosité du pixel d'origine (le relief
 *     reste lisible) ;</li>
 *     <li>la taille des motifs suit la résolution (gemme de 3 pixels en 16 px, taillée en losange au-delà) ; les
 *     filets restent fins d'un pixel.</li>
 * </ul>
 * <p>Repère : t = x - y (le long de l'arme), u = x + y - (taille - 1) (écart à l'axe).</p>
 */
@SideOnly(Side.CLIENT)
final class ModifierArt {

    static final int HANDLE = 1;
    static final int HEAD = 2;
    static final int EXTRA = 3;

    /** Palettes (sombre -> clair). */
    private static final Map<String, int[]> PALETTES = new HashMap<>();

    static {
        pal("haste", 0x6E0808, 0xBE1919, 0xFA503C, 0xFFBEAA);
        pal("diamond", 0x105456, 0x24A098, 0x60E4D8, 0xD7FFFA);
        pal("emerald", 0x064018, 0x128434, 0x3CCD5C, 0xC3FFCD);
        pal("reinforced", 0x221234, 0x462A68, 0x7458A6, 0xB09CDE);
        pal("sharpness", 0x96848E, 0xDACCD2, 0xF8F0F4, 0xFFFFFF);
        pal("smite", 0x68460A, 0xBA881C, 0xEEC648, 0xFFF2B0);
        pal("bane_of_arthopods", 0x3A1026, 0x742246, 0xA8426C, 0xE29CBA);
        pal("beheading", 0x12222E, 0x1C585C, 0x48A096, 0xBAFAE6);
        pal("fiery", 0x821A00, 0xD65808, 0xFFA828, 0xFFEE8C);
        pal("necrotic", 0x240006, 0x600A14, 0x941C28, 0xCC5454);
        pal("knockback", 0x563A14, 0x946C30, 0xC8A050, 0xF2D48E);
        pal("luck", 0x0E1C58, 0x1E3CA8, 0x4870E6, 0xB0CCFF);
        pal("silktouch", 0x94763A, 0xDABC76, 0xF8EABC, 0xFFFFF2);
        pal("glowing", 0xFFD66E, 0xFFE896, 0xFFF6C8, 0xFFFFEB);
        pal("shulking", 0x3A1446, 0x763A80, 0xAA6CB4, 0xE8C0F0);
        pal("soulbound", 0x8C8CA8, 0xCECEE4, 0xF4F4FF, 0xFFFFFF);
        pal("mending_moss", 0x1C3A0E, 0x3A6C1C, 0x629E30, 0xA8D258);
        pal("webbed", 0x6E6E78, 0xD2D2D2, 0xF0F0F0, 0xFFFFFF);
        pal("blasting", 0x78120C, 0xC42818, 0xECECE4, 0xFFFFFF);
        pal("fortify", 0x5A5A5A, 0x969696, 0xD2D2D2, 0xFAFAFA);
        pal("fins", 0x1C5656, 0x3A8A7E, 0x6CBCA8, 0xBEF0DE);
    }

    private static void pal(String name, int... colors) {
        PALETTES.put(name, colors);
    }

    private ModifierArt() {
    }

    /** Forme d'une arme : rôle (manche/tête/garde) et luminosité de chaque pixel. */
    static final class Geo {
        final int size;
        final int[] role;
        final int[] lum;

        Geo(int size, int[] role, int[] lum) {
            this.size = size;
            this.role = role;
            this.lum = lum;
        }
    }

    /**
     * Dessine le modificateur sur l'arme.
     *
     * @return pixels ARGB (transparents hors motif), ou null si le modificateur n'a pas de dessin.
     */
    static int[] paint(String modifier, Geo geo, long seed) {
        int[] pal = PALETTES.get(modifier);
        if (pal == null) {
            return null;
        }
        Shape sh = new Shape(geo);
        if (sh.head.isEmpty()) {
            return null;
        }
        Canvas out = new Canvas(sh);
        Random rnd = new Random(seed);

        switch (modifier) {
            case "diamond": // tranchant serti de diamant
                for (int p : sh.edge(true)) {
                    if (sh.s(p) > 0.08) {
                        out.set(p, shade(pal, sh.lum(p)));
                    }
                }
                out.set(sh.tip(), pal[3]);
                break;
            case "sharpness": // fil affûté, quartz brillant qui s'intensifie vers la pointe
                for (int p : sh.edge(true)) {
                    double s = sh.s(p);
                    if (s > 0.2) {
                        out.set(p, pal[s < 0.7 ? 2 : 3]);
                    }
                }
                for (int p : sh.edge(false)) {
                    if (sh.s(p) > 0.7) {
                        out.set(p, pal[1]);
                    }
                }
                break;
            case "fiery": // tranchant incandescent, rouge à la base, jaune à la pointe
                for (int p : sh.edge(true)) {
                    out.set(p, pal[Math.min(3, (int) (sh.s(p) * 3.99))]);
                }
                for (int p : sh.edge(false)) {
                    if (sh.s(p) > 0.55) {
                        out.set(p, pal[1]);
                    }
                }
                break;
            case "beheading": // pointe trempée à l'ender : sarcelle sur le fil, violet au dos
                for (int p : sh.edge(true)) {
                    if (sh.s(p) > 0.45) {
                        out.set(p, pal[sh.s(p) < 0.8 ? 2 : 3]);
                    }
                }
                for (int p : sh.edge(false)) {
                    if (sh.s(p) > 0.45) {
                        out.set(p, PALETTES.get("shulking")[1]);
                    }
                }
                break;
            case "fortify": // renfort du dos de la lame (gris : Tinkers le colore selon le matériau)
                for (int p : sh.edge(false)) {
                    double s = sh.s(p);
                    if (s > 0.1 && s < 0.75) {
                        out.set(p, shade(pal, sh.lum(p)));
                    }
                }
                break;
            case "fins": // nageoire de prismarine sur le dos de la tête
                int n = 0;
                for (int p : sorted(sh.edge(false), sh)) {
                    double s = sh.s(p);
                    if (s > 0.15 && s < 0.5) {
                        out.set(p, pal[1 + (n++ % 2)]);
                    }
                }
                break;
            case "haste": // trois rubis de redstone sertis dans la gorge
                for (int p : spaced(sh.fuller(0.18, 0.85), 3)) {
                    if (sh.k > 1) {
                        out.setIfHead(sh.x(p) + 1, sh.y(p), pal[1]);
                        out.setIfHead(sh.x(p), sh.y(p) + 1, pal[1]);
                        out.set(p, pal[3]);
                    } else {
                        out.set(p, pal[2]);
                    }
                }
                break;
            case "smite": // gorge dorée, gravure sacrée
                for (int p : sh.fuller(0.12, 0.9)) {
                    out.set(p, shade(pal, Math.max(140, sh.lum(p))));
                }
                break;
            case "necrotic": // veines de sang le long de la gorge
                List<Integer> veins = sh.fuller(0.1, 0.8);
                for (int i = 0; i < veins.size(); i++) {
                    if (i % 3 != 2) {
                        out.set(veins.get(i), pal[1 + (i % 2)]);
                    }
                }
                break;
            case "shulking": // perles de shulker sur la gorge
                for (int p : spaced(sh.fuller(0.2, 0.9), 4)) {
                    out.set(p, pal[2]);
                }
                break;
            case "emerald": // émeraude sertie dans la garde
                gem(sh, out, sh.junction(), pal, sh.k - 1);
                break;
            case "soulbound": // étoile du nether au cœur de la garde
                star(sh, out, sh.junction(), pal);
                break;
            case "luck": { // lapis serti dans le pommeau, entouré d'or
                int at = sh.pommel();
                gem(sh, out, at, pal, sh.k - 1);
                if (sh.k == 1) {
                    out.setIfSolid(sh.x(at) + 1, sh.y(at), PALETTES.get("smite")[2]);
                    out.setIfSolid(sh.x(at), sh.y(at) - 1, PALETTES.get("smite")[2]);
                }
                break;
            }
            case "blasting": { // pommeau rayé rouge et blanc
                int at = sh.pommel();
                double radius = 1.5 * sh.k;
                for (int p : sh.all) {
                    double dx = sh.x(p) - sh.x(at);
                    double dy = sh.y(p) - sh.y(at);
                    if (dx * dx + dy * dy <= radius * radius) {
                        out.set(p, pal[(sh.x(p) + sh.y(p)) % 2 == 1 ? 1 : 2]);
                    }
                }
                break;
            }
            case "silktouch": // poignée entièrement gainée de soie crème, spires alternées
                for (int p : sh.grip()) {
                    boolean band = Math.floorMod(Math.floorDiv(sh.t(p), sh.k), 2) == 1;
                    boolean light = sh.lum(p) >= 128;
                    out.set(p, pal[(light ? 2 : 1) - (band ? 0 : 1)]);
                }
                break;
            case "knockback": { // bagues de laiton sur le manche
                List<Integer> ts = new ArrayList<>();
                for (int p : sh.handle) {
                    if (!ts.contains(sh.t(p))) {
                        ts.add(sh.t(p));
                    }
                }
                ts.sort(Integer::compare);
                if (!ts.isEmpty()) {
                    for (double frac : new double[]{0.35, 0.7}) {
                        int t = ts.get((int) (frac * (ts.size() - 1)));
                        for (int p : sh.handle) {
                            if (sh.t(p) >= t && sh.t(p) < t + sh.k) {
                                out.set(p, shade(pal, Math.min(255, sh.lum(p) + 30)));
                            }
                        }
                    }
                }
                break;
            }
            case "reinforced": // plaques d'obsidienne à la base de la tête
                for (double s : new double[]{0.04, 0.16}) {
                    int t = (int) Math.round(sh.h0 + s * (sh.h1 - sh.h0));
                    for (int p : sh.head) {
                        if (sh.t(p) >= t && sh.t(p) < t + sh.k) {
                            out.set(p, shade(pal, sh.lum(p)));
                        }
                    }
                }
                break;
            case "mending_moss": { // mousse qui pousse depuis la garde
                List<Integer> base = new ArrayList<>();
                for (int p : sh.head) {
                    if (sh.s(p) < 0.35) {
                        base.add(p);
                    }
                }
                base.sort(Comparator.comparingDouble((Integer p) -> sh.s(p)).thenComparingInt(p -> p));
                int step = Math.max(1, base.size() / (4 * sh.k));
                int count = 0;
                for (int i = 0; i < base.size() && count < 5 * sh.k; i += step, count++) {
                    out.set(base.get(i), pal[1 + rnd.next(3)]);
                }
                break;
            }
            case "bane_of_arthopods": // gouttes de venin le long du tranchant
                for (int p : spaced(filter(sorted(sh.edge(false), sh), sh, 0.3, 2.0), 3)) {
                    out.set(p, pal[2]);
                    out.setIfEmpty(sh.x(p), sh.y(p) + 1, pal[1]);
                }
                break;
            case "glowing": { // éclats de lumière autour de la lame, et la pointe qui brille
                out.set(sh.tip(), pal[3]);
                Set<Integer> around = new LinkedHashSet<>();
                for (int p : sh.outline(sh.head)) {
                    for (int d = -1; d <= 1; d += 2) {
                        int qx = sh.x(p) + d * (sh.k + 1);
                        int qy = sh.y(p) + d * (sh.k + 1);
                        if (sh.inside(qx, qy) && !sh.solid(qx, qy)) {
                            around.add(qy * sh.size + qx);
                        }
                    }
                }
                for (int q : spaced(sorted(new ArrayList<>(around), sh), 3)) {
                    out.set(q, pal[3]);
                    if (sh.k > 1) {
                        out.setIfEmpty(sh.x(q) + 1, sh.y(q), pal[1]);
                        out.setIfEmpty(sh.x(q) - 1, sh.y(q), pal[1]);
                        out.setIfEmpty(sh.x(q), sh.y(q) + 1, pal[1]);
                        out.setIfEmpty(sh.x(q), sh.y(q) - 1, pal[1]);
                    }
                }
                break;
            }
            case "webbed": // résille de fils de soie d'araignée à la base de la lame
                for (int p : sh.head) {
                    if (sh.s(p) < 0.3 && (Math.floorMod(sh.t(p), 3 * sh.k) == 0
                            || Math.floorMod(sh.x(p) + sh.y(p), 4 * sh.k) == 0)) {
                        out.set(p, sh.lum(p) < 160 ? pal[3] : pal[0]);
                    }
                }
                break;
            default:
                return null;
        }
        return out.pixels;
    }

    // ------------------------------------------------------------------ motifs

    /** Gemme : 3 pixels en diagonale en 16 px, losange taillé et ombré au-delà. Jamais hors de la silhouette. */
    private static void gem(Shape sh, Canvas out, int at, int[] pal, int r) {
        int cx = sh.x(at);
        int cy = sh.y(at);
        if (r <= 0) {
            out.set(at, pal[2]);
            out.setIfSolid(cx - 1, cy - 1, pal[3]);
            out.setIfSolid(cx + 1, cy + 1, pal[1]);
            return;
        }
        for (int dy = -r - 1; dy <= r + 1; dy++) {
            for (int dx = -r - 1; dx <= r + 1; dx++) {
                int d = Math.abs(dx) + Math.abs(dy);
                if (d > r + 1) {
                    continue;
                }
                int color = d == r + 1 ? pal[0] : (dx + dy < 0 ? pal[2] : pal[1]);
                out.setIfSolid(cx + dx, cy + dy, color);
            }
        }
        out.setIfSolid(cx - (r - 1), cy - (r - 1), pal[3]);
    }

    private static void star(Shape sh, Canvas out, int at, int[] pal) {
        int cx = sh.x(at);
        int cy = sh.y(at);
        out.set(at, pal[3]);
        for (int i = 1; i <= sh.k; i++) {
            int c = i < sh.k ? pal[2] : pal[1];
            out.setIfSolid(cx + i, cy, c);
            out.setIfSolid(cx - i, cy, c);
            out.setIfSolid(cx, cy + i, c);
            out.setIfSolid(cx, cy - i, c);
        }
    }

    /** Couleur de la palette selon la luminosité du pixel d'origine. */
    private static int shade(int[] pal, int lum) {
        return pal[Math.min(3, Math.max(0, lum) * 4 / 256)];
    }

    /** n éléments répartis régulièrement dans la liste. */
    private static List<Integer> spaced(List<Integer> points, int n) {
        if (points.size() <= n) {
            return points;
        }
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(points.get((int) Math.round((i + 0.5) * points.size() / n - 0.5)));
        }
        return out;
    }

    /** Tri le long de l'arme (puis par position), pour des résultats stables. */
    private static List<Integer> sorted(List<Integer> points, Shape sh) {
        List<Integer> copy = new ArrayList<>(points);
        copy.sort(Comparator.comparingInt((Integer p) -> sh.t(p)).thenComparingInt(p -> sh.x(p)).thenComparingInt(p -> sh.y(p)));
        return copy;
    }

    private static List<Integer> filter(List<Integer> points, Shape sh, double sMin, double sMax) {
        List<Integer> out = new ArrayList<>();
        for (int p : points) {
            double s = sh.s(p);
            if (s > sMin && s < sMax) {
                out.add(p);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ analyse de la forme

    private static final class Shape {
        final int size;
        /** Facteur de résolution : 1 en 16 px, 2 en 32 px, 4 en 64 px. */
        final int k;
        final int[] role;
        final int[] lums;
        final List<Integer> all = new ArrayList<>();
        final List<Integer> head = new ArrayList<>();
        final List<Integer> extra = new ArrayList<>();
        final List<Integer> handle = new ArrayList<>();
        final int t0;
        final int t1;
        final int h0;
        final int h1;
        /** Pour chaque t de la tête : {u moyen, u min, u max}. */
        final Map<Integer, double[]> centre = new HashMap<>();

        Shape(Geo geo) {
            size = geo.size;
            k = Math.max(1, size / 16);
            role = geo.role;
            lums = geo.lum;
            for (int i = 0; i < role.length; i++) {
                if (role[i] == 0) {
                    continue;
                }
                all.add(i);
                if (role[i] == HEAD) {
                    head.add(i);
                } else if (role[i] == EXTRA) {
                    extra.add(i);
                } else {
                    handle.add(i);
                }
            }
            int a = Integer.MAX_VALUE;
            int b = Integer.MIN_VALUE;
            for (int p : all) {
                a = Math.min(a, t(p));
                b = Math.max(b, t(p));
            }
            t0 = a;
            t1 = b;
            List<Integer> h = head.isEmpty() ? all : head;
            a = Integer.MAX_VALUE;
            b = Integer.MIN_VALUE;
            Map<Integer, List<Integer>> us = new HashMap<>();
            for (int p : h) {
                a = Math.min(a, t(p));
                b = Math.max(b, t(p));
                us.computeIfAbsent(t(p), key -> new ArrayList<>()).add(u(p));
            }
            h0 = a;
            h1 = b;
            for (Map.Entry<Integer, List<Integer>> e : us.entrySet()) {
                double sum = 0;
                int lo = Integer.MAX_VALUE;
                int hi = Integer.MIN_VALUE;
                for (int u : e.getValue()) {
                    sum += u;
                    lo = Math.min(lo, u);
                    hi = Math.max(hi, u);
                }
                centre.put(e.getKey(), new double[]{sum / e.getValue().size(), lo, hi});
            }
        }

        int x(int p) {
            return p % size;
        }

        int y(int p) {
            return p / size;
        }

        int t(int p) {
            return x(p) - y(p);
        }

        int u(int p) {
            return x(p) + y(p) - (size - 1);
        }

        int lum(int p) {
            return lums[p];
        }

        /** Position le long de la tête : 0 = base, 1 = pointe. */
        double s(int p) {
            return (double) (t(p) - h0) / Math.max(1, h1 - h0);
        }

        /** Position le long de toute l'arme. */
        double bigS(int p) {
            return (double) (t(p) - t0) / Math.max(1, t1 - t0);
        }

        boolean inside(int x, int y) {
            return x >= 0 && y >= 0 && x < size && y < size;
        }

        boolean solid(int x, int y) {
            return inside(x, y) && role[y * size + x] != 0;
        }

        List<Integer> outline(List<Integer> pixels) {
            List<Integer> out = new ArrayList<>();
            for (int p : pixels) {
                int x = x(p);
                int y = y(p);
                if (!solid(x + 1, y) || !solid(x - 1, y) || !solid(x, y + 1) || !solid(x, y - 1)) {
                    out.add(p);
                }
            }
            return out;
        }

        /** Contour de la tête côté haut-gauche (upper, le « fil » éclairé) ou bas-droit (le dos). */
        List<Integer> edge(boolean upper) {
            List<Integer> out = new ArrayList<>();
            for (int p : outline(head)) {
                double c = centre.get(t(p))[0];
                if ((u(p) < c) == upper) {
                    out.add(p);
                }
            }
            return out;
        }

        /** Gorge : pixel le plus central de la tête à chaque t, là où elle est assez large. */
        List<Integer> fuller(double s0, double s1) {
            List<Integer> out = new ArrayList<>();
            for (int minWidth : new int[]{4, 2}) {
                out = new ArrayList<>();
                List<Integer> ts = new ArrayList<>(centre.keySet());
                ts.sort(Integer::compare);
                for (int t : ts) {
                    double[] c = centre.get(t);
                    double s = (double) (t - h0) / Math.max(1, h1 - h0);
                    if (s < s0 || s > s1 || c[2] - c[1] < minWidth) {
                        continue;
                    }
                    int best = -1;
                    for (int p : head) {
                        if (t(p) == t && (best < 0 || Math.abs(u(p) - c[0]) < Math.abs(u(best) - c[0])
                                || (Math.abs(u(p) - c[0]) == Math.abs(u(best) - c[0]) && x(p) < x(best)))) {
                            best = p;
                        }
                    }
                    out.add(best);
                }
                if (out.size() >= 3) {
                    return out;
                }
            }
            return out;
        }

        /** Poignée : partie du manche juste sous la garde (ou la tête). */
        List<Integer> grip() {
            List<Integer> out = new ArrayList<>();
            if (handle.isEmpty()) {
                return out;
            }
            int top = Integer.MIN_VALUE;
            for (int p : handle) {
                top = Math.max(top, t(p));
            }
            for (int p : handle) {
                if (t(p) > top - 7 * k) {
                    out.add(p);
                }
            }
            return out;
        }

        int tip() {
            int best = head.get(0);
            for (int p : head) {
                if (t(p) > t(best)) {
                    best = p;
                }
            }
            return best;
        }

        /** Centre de la garde (côté tête, pas le pommeau), ou base de la tête sans garde. */
        int junction() {
            List<Integer> pts = new ArrayList<>();
            if (!extra.isEmpty()) {
                int top = Integer.MIN_VALUE;
                for (int p : extra) {
                    top = Math.max(top, t(p));
                }
                for (int p : extra) {
                    if (t(p) >= top - 3 * k) {
                        pts.add(p);
                    }
                }
            } else {
                for (int p : head) {
                    if (s(p) <= 0.1) {
                        pts.add(p);
                    }
                }
            }
            if (pts.isEmpty()) {
                pts.addAll(head);
            }
            return nearestToCentroid(pts);
        }

        int pommel() {
            List<Integer> low = new ArrayList<>();
            for (int p : all) {
                if (bigS(p) <= 0.06) {
                    low.add(p);
                }
            }
            return nearestToCentroid(low.isEmpty() ? all : low);
        }

        private int nearestToCentroid(List<Integer> pts) {
            double cx = 0;
            double cy = 0;
            for (int p : pts) {
                cx += x(p);
                cy += y(p);
            }
            cx /= pts.size();
            cy /= pts.size();
            int best = pts.get(0);
            double bestD = Double.MAX_VALUE;
            for (int p : pts) {
                double d = (x(p) - cx) * (x(p) - cx) + (y(p) - cy) * (y(p) - cy);
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
            return best;
        }
    }

    /** Pixels de sortie, avec les règles de pose (silhouette / tête / vide). */
    private static final class Canvas {
        final Shape sh;
        final int[] pixels;

        Canvas(Shape sh) {
            this.sh = sh;
            this.pixels = new int[sh.size * sh.size];
        }

        void set(int p, int rgb) {
            pixels[p] = 0xFF000000 | rgb;
        }

        void setIfSolid(int x, int y, int rgb) {
            if (sh.solid(x, y)) {
                set(y * sh.size + x, rgb);
            }
        }

        void setIfHead(int x, int y, int rgb) {
            if (sh.inside(x, y) && sh.role[y * sh.size + x] == HEAD) {
                set(y * sh.size + x, rgb);
            }
        }

        void setIfEmpty(int x, int y, int rgb) {
            if (sh.inside(x, y) && !sh.solid(x, y)) {
                set(y * sh.size + x, rgb);
            }
        }
    }

    /** Petit générateur déterministe (même résultat à chaque chargement). */
    private static final class Random {
        private long state;

        Random(long seed) {
            state = seed & 0x7FFFFFFFL;
            if (state == 0) {
                state = 1;
            }
        }

        int next(int bound) {
            state = (state * 1103515245L + 12345L) & 0x7FFFFFFFL;
            return (int) (state % bound);
        }
    }
}
