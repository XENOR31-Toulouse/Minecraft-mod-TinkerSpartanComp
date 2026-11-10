package tconspartan.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.PageData;
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
