package tconspartan.client;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.PageData;
import slimeknights.mantle.client.book.data.SectionData;
import slimeknights.mantle.client.book.repository.FileRepository;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.book.TinkerBook;
import slimeknights.tconstruct.library.book.content.ContentListing;
import slimeknights.tconstruct.library.book.content.ContentTool;
import slimeknights.tconstruct.library.book.sectiontransformer.ContentListingSectionTransformer;
import tconspartan.TConSpartan;

/**
 * Section « Arsenal spartiate » du livre de Tinkers (Materials and You).
 *
 * <p>Les pages sont dans {@code assets/tconspartan/book/} : {@code index.json} déclare la section,
 * {@code sections/spartan.json} liste ses pages (textes + une page « tool » par arme), et les textes sont dans
 * {@code <langue>/spartan/}. Mantle ajoute la section au livre de Tinkers ; ce transformateur lui ajoute une table
 * des matières où les armes apparaissent sous leur nom traduit.</p>
 */
@SideOnly(Side.CLIENT)
public final class SpartanBook {

    private SpartanBook() {
    }

    public static void register() {
        TinkerBook.INSTANCE.addRepository(new FileRepository(TConSpartan.MODID + ":book"));
        TinkerBook.INSTANCE.addTransformer(new Contents());
        if (Boolean.getBoolean("tconspartan.checkBook")) {
            MinecraftForge.EVENT_BUS.register(new SelfCheck());
        }
    }

    /**
     * Option de développement ({@code ./gradlew runClient -PcheckBook}) : charge le livre à l'arrivée sur le menu
     * principal et écrit dans le journal les pages de la section, ou les erreurs de chargement.
     */
    public static final class SelfCheck {
        private boolean done;

        @SubscribeEvent
        public void onGui(GuiOpenEvent event) {
            if (done || !(event.getGui() instanceof GuiMainMenu)) {
                return;
            }
            done = true;
            // Noms traduits (une clé brute ici = fichiers de langue non chargés, cf. pack.mcmeta)
            TConSpartan.LOG.info("[book] weapon name: '{}'", tconspartan.tools.SpartanWeapon.KATANA.tool.getLocalizedName());
            BookData book = TinkerBook.INSTANCE;
            book.load();
            for (SectionData section : book.sections) {
                boolean ours = "spartan".equals(section.name);
                for (PageData page : section.pages) {
                    String type = page.content == null ? "null" : page.content.getClass().getSimpleName();
                    if (ours || type.contains("Error")) {
                        TConSpartan.LOG.info("[book] {} / {} : {}", section.name, page.getTitle(), type);
                    }
                }
            }
        }
    }

    /** Table des matières de la section : pages de texte par titre, armes par nom traduit. */
    private static final class Contents extends ContentListingSectionTransformer {
        Contents() {
            super("spartan");
        }

        @Override
        protected void processPage(BookData book, ContentListing listing, PageData page) {
            if (page.content instanceof ContentTool) {
                String toolName = ((ContentTool) page.content).toolName;
                TinkerRegistry.getTools().stream()
                        .filter(tool -> toolName.equals(tool.getIdentifier()))
                        .findFirst()
                        .ifPresent(tool -> listing.addEntry(tool.getLocalizedName(), page));
            } else {
                super.processPage(book, listing, page);
            }
        }
    }
}
