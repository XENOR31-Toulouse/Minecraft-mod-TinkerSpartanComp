package tconspartan.tools;

import slimeknights.tconstruct.library.tools.ToolPart;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.function.Supplier;

/**
 * Raccourcis vers les pièces existantes de Tinkers' Construct (résolues tard : les champs de TinkerTools ne sont
 * remplis qu'à l'enregistrement des items de Tinkers).
 */
public final class TinkerParts {
    public static final Supplier<ToolPart> ROD = () -> TinkerTools.toolRod;
    public static final Supplier<ToolPart> TOUGH_ROD = () -> TinkerTools.toughToolRod;
    public static final Supplier<ToolPart> BINDING = () -> TinkerTools.binding;
    public static final Supplier<ToolPart> TOUGH_BINDING = () -> TinkerTools.toughBinding;
    public static final Supplier<ToolPart> CROSS_GUARD = () -> TinkerTools.crossGuard;
    public static final Supplier<ToolPart> HAND_GUARD = () -> TinkerTools.handGuard;
    public static final Supplier<ToolPart> WIDE_GUARD = () -> TinkerTools.wideGuard;
    public static final Supplier<ToolPart> LARGE_PLATE = () -> TinkerTools.largePlate;
    public static final Supplier<ToolPart> KNIFE_BLADE = () -> TinkerTools.knifeBlade;
    public static final Supplier<ToolPart> SWORD_BLADE = () -> TinkerTools.swordBlade;
    public static final Supplier<ToolPart> LARGE_SWORD_BLADE = () -> TinkerTools.largeSwordBlade;
    public static final Supplier<ToolPart> AXE_HEAD = () -> TinkerTools.axeHead;
    public static final Supplier<ToolPart> BROAD_AXE_HEAD = () -> TinkerTools.broadAxeHead;
    public static final Supplier<ToolPart> PICK_HEAD = () -> TinkerTools.pickHead;
    public static final Supplier<ToolPart> HAMMER_HEAD = () -> TinkerTools.hammerHead;
    public static final Supplier<ToolPart> SCYTHE_HEAD = () -> TinkerTools.scytheHead;
    public static final Supplier<ToolPart> KAMA_HEAD = () -> TinkerTools.kamaHead;
    public static final Supplier<ToolPart> ARROW_HEAD = () -> TinkerTools.arrowHead;

    private TinkerParts() {
    }
}
