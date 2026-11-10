package tconspartan.client;

import com.oblivioussp.spartanweaponry.api.IWeaponPropertyContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.PageData;
import slimeknights.mantle.client.book.data.SectionData;
import slimeknights.mantle.client.gui.book.GuiBook;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.book.TinkerBook;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.tools.ToolCore;
import slimeknights.tconstruct.library.utils.ToolHelper;
import tconspartan.TConSpartan;
import tconspartan.throwing.Ammo;
import tconspartan.tools.SpartanWeapon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Vérifications automatiques pour le développement, activées par {@code ./gradlew runClient -PdevChecks}
 * (résultats dans le journal, lignes {@code [check]}) :
 * <ul>
 *     <li>au menu principal : chargement du livre et traduction des noms ;</li>
 *     <li>une fois en jeu : munitions des armes de lancer, tableau des stats (comparé à Spartan et Tinkers),
 *     puis captures d'écran de chaque double page de la section du livre ({@code screenshots/tconspartan-book-*.png}).</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class DevChecks {

    private boolean menuDone;
    private boolean worldDone;
    private int bookStep = -1;
    private int bookTimer;
    private int bookFirstPage;
    private int bookLastPage;
    private GuiBook gui;

    private DevChecks() {
    }

    public static void registerIfEnabled() {
        if (Boolean.getBoolean("tconspartan.devChecks")) {
            MinecraftForge.EVENT_BUS.register(new DevChecks());
        }
    }

    private static void log(String format, Object... args) {
        TConSpartan.LOG.info("[check] " + format, args);
    }

    // ------------------------------------------------------------------ menu principal : livre et traductions

    @SubscribeEvent
    public void onGui(GuiOpenEvent event) {
        if (menuDone || !(event.getGui() instanceof GuiMainMenu)) {
            return;
        }
        menuDone = true;
        log("weapon name: '{}' (a raw key means the lang files are not loaded)", SpartanWeapon.KATANA.tool.getLocalizedName());
        BookData book = TinkerBook.INSTANCE;
        book.load();
        int pages = 0;
        for (SectionData section : book.sections) {
            for (PageData page : section.pages) {
                String type = page.content == null ? "null" : page.content.getClass().getSimpleName();
                if ("spartan".equals(section.name)) {
                    pages++;
                }
                if (type.contains("Error")) {
                    log("BOOK ERROR in {} / {}", section.name, page.getTitle());
                }
            }
        }
        log("book: {} pages in the spartan section", pages);
    }

    // ------------------------------------------------------------------ en jeu

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || mc.player == null || mc.world == null) {
            return;
        }
        if (!worldDone) {
            worldDone = true;
            try {
                checkAmmo();
                statsTable();
            } catch (Exception e) {
                log("FAIL: exception {}", e.toString());
                TConSpartan.LOG.error("[check]", e);
            }
            bookStep = 0;
            bookTimer = 40;
            return;
        }
        if (bookStep >= 0) {
            bookScreenshots(mc);
        }
    }

    private static Material mat(String id) {
        Material m = TinkerRegistry.getMaterial(id);
        if (m == null || m == Material.UNKNOWN) {
            throw new IllegalStateException("material " + id + " missing");
        }
        return m;
    }

    private static ItemStack build(SpartanWeapon weapon, Material handle, Material head, Material extra) {
        List<Material> materials = new ArrayList<>();
        materials.add(handle);
        materials.add(head);
        if (weapon.hasExtraPart()) {
            materials.add(extra);
        }
        return weapon.tool.buildItem(materials);
    }

    private void checkAmmo() {
        Minecraft mc = Minecraft.getMinecraft();
        String[][] combos = {{"iron", "iron"}, {"wood", "flint"}, {"cobalt", "cobalt"}, {"wood", "paper"}};
        for (SpartanWeapon weapon : new SpartanWeapon[]{SpartanWeapon.THROWING_KNIFE, SpartanWeapon.THROWING_AXE, SpartanWeapon.JAVELIN}) {
            StringBuilder counts = new StringBuilder();
            for (String[] c : combos) {
                ItemStack s = build(weapon, mat(c[0]), mat(c[1]), mat(c[1]));
                counts.append(c[1]).append(" on ").append(c[0]).append("=").append(Ammo.max(s)).append("  ");
            }
            log("ammo {}: {}", weapon.id(), counts);
        }

        ItemStack knife = build(SpartanWeapon.THROWING_KNIFE, mat("iron"), mat("iron"), mat("iron"));
        int max = Ammo.max(knife);
        check("new stack is full", Ammo.get(knife) == max, Ammo.get(knife) + "/" + max);
        check("projectile copy holds 1", Ammo.get(Ammo.forProjectile(knife)) == 1, "" + Ammo.get(Ammo.forProjectile(knife)));
        check("projectile copy keeps the same max", Ammo.max(Ammo.forProjectile(knife)) == max, "" + Ammo.max(Ammo.forProjectile(knife)));
        int uses = 0;
        while (Ammo.get(knife) > 0 && uses < 1000) {
            Ammo.use(knife, mc.player);
            uses++;
        }
        check("throws until empty == max", uses == max, uses + " throws for " + max);
        check("empty stack is 'broken' (Empty)", ToolHelper.isBroken(knife), "broken=" + ToolHelper.isBroken(knife));
        ToolHelper.unbreakTool(knife);
        ToolHelper.healTool(knife, Ammo.perAmmo(knife), null);
        check("picking one up gives 1 back", Ammo.get(knife) == 1 && !ToolHelper.isBroken(knife), "ammo=" + Ammo.get(knife));
        check("name shows the count", knife.getDisplayName().endsWith("×1"), knife.getDisplayName());
    }

    private static void check(String what, boolean ok, String detail) {
        log("{} {} ({})", ok ? "PASS" : "FAIL", what, detail);
    }

    /** Dégâts (avec le +1 du joueur), vitesse et durabilité en fer, comparés à Spartan et Tinkers. */
    private void statsTable() {
        Material iron = mat("iron");
        log("stats, iron everywhere: weapon | damage | speed | durability || Spartan iron: damage | speed");
        for (SpartanWeapon weapon : SpartanWeapon.values()) {
            ItemStack stack = build(weapon, iron, iron, iron);
            double[] ours = attributes(stack);
            String spartan = "-";
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("spartanweaponry", weapon.spartanName() + "_iron"));
            if (item instanceof IWeaponPropertyContainer) {
                double[] sp = attributes(new ItemStack(item));
                spartan = String.format("%.1f | %.2f", sp[0], sp[1]);
            }
            log(String.format("%-16s | %5.1f | %.2f | %4d || %s", weapon.id(), ours[0], ours[1],
                    ToolHelper.getMaxDurability(stack), spartan));
        }
        for (ToolCore tool : TinkerRegistry.getTools()) {
            String id = tool.getIdentifier();
            if ("broadsword".equals(id) || "longsword".equals(id) || "rapier".equals(id) || "cleaver".equals(id)) {
                List<Material> mats = new ArrayList<>(Collections.nCopies(tool.getRequiredComponents().size(), iron));
                ItemStack stack = tool.buildItem(mats);
                double[] t = attributes(stack);
                log(String.format("tinkers %-8s | %5.1f | %.2f | %4d", id, t[0], t[1], ToolHelper.getMaxDurability(stack)));
            }
        }
    }

    /** {dégâts totaux, vitesse} d'une arme tenue en main (valeurs de base du joueur : 1 et 4). */
    private static double[] attributes(ItemStack stack) {
        double damage = 1;
        double speed = 4;
        for (java.util.Map.Entry<String, AttributeModifier> e : stack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND).entries()) {
            if (e.getKey().equals(SharedMonsterAttributes.ATTACK_DAMAGE.getName())) {
                damage += e.getValue().getAmount();
            } else if (e.getKey().equals(SharedMonsterAttributes.ATTACK_SPEED.getName())) {
                speed += e.getValue().getAmount();
            }
        }
        return new double[]{damage, speed};
    }

    // ------------------------------------------------------------------ captures du livre

    private void bookScreenshots(Minecraft mc) {
        if (--bookTimer > 0) {
            return;
        }
        BookData book = TinkerBook.INSTANCE;
        if (bookStep == 0) {
            book.load();
            // Mantle lit la page enregistrée dans l'objet livre : il faut un vrai livre Tinkers.
            Item bookItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("tconstruct", "book"));
            gui = new GuiBook(book, new ItemStack(bookItem));
            mc.displayGuiScreen(gui);
            bookFirstPage = book.findPageNumber("spartan.spartan");
            bookLastPage = bookFirstPage + 27;
            log("book: section starts at page {}", bookFirstPage);
            gui.openPage(bookFirstPage);
            bookStep = 1;
            bookTimer = 20;
            return;
        }
        int page = bookFirstPage + (bookStep - 1) * 2;
        if (page > bookLastPage || mc.currentScreen != gui) {
            log("book: screenshots done");
            mc.displayGuiScreen(null);
            bookStep = -1;
            return;
        }
        ScreenShotHelper.saveScreenshot(mc.gameDir, String.format("tconspartan-book-%02d.png", bookStep),
                mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        gui.openPage(page + 2);
        bookStep++;
        bookTimer = 20;
    }
}
