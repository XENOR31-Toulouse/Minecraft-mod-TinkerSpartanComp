"""Textes de l'addon (noms, descriptions, livre Tinkers) en anglais et en français, générés depuis une seule table."""
import json
import pathlib
import sys

ROOT = pathlib.Path(sys.argv[1]) / "src/main/resources/assets/tconspartan"

# id: (nom EN, nom FR, description EN, description FR, texte livre EN, texte livre FR, propriétés EN, propriétés FR)
W = {
    "dagger": (
        "Dagger", "Dague",
        "A short, quick blade that strikes three times harder from behind.\\nHold right-click to throw it.",
        "Une lame courte et vive, trois fois plus mortelle dans le dos.\\nMaintenez clic droit pour la lancer.",
        "The assassin's companion. Light and fast, it deals triple damage when you strike an enemy from behind, and can be thrown in a pinch.",
        "La compagne des assassins. Légère et rapide, elle inflige le triple de dégâts quand on frappe un ennemi dans le dos, et se lance au besoin.",
        ["Backstab: triple damage", "Throwable", "Fast"],
        ["Dans le dos : dégâts triplés", "Se lance", "Rapide"]),
    "parrying_dagger": (
        "Parrying Dagger", "Dague de parade",
        "An off-hand blade made to turn aside blows.\\nHold right-click to parry melee attacks.",
        "Une lame de seconde main faite pour détourner les coups.\\nMaintenez clic droit pour parer les attaques de mêlée.",
        "Built for defence rather than offence. Hold right-click to raise it: melee blows are turned aside and only chip at its durability.",
        "Pensée pour la défense plus que l'attaque. Maintenez clic droit pour la lever : les coups de mêlée sont détournés et n'entament que sa durabilité.",
        ["Parries melee attacks", "Fast"],
        ["Pare les attaques de mêlée", "Rapide"]),
    "katana": (
        "Katana", "Katana",
        "A swift curved sword that sweeps through crowds and bites deep into unarmoured foes.\\nBest wielded with both hands.",
        "Un sabre courbe et vif, qui balaie les foules et mord profondément les ennemis sans armure.\\nÀ manier à deux mains.",
        "Elegant and deadly. Its sweep cuts through groups, and enemies without a chestplate take double damage. Keep the other hand free.",
        "Élégant et mortel. Son balayage traverse les groupes, et les ennemis sans plastron subissent le double de dégâts. Gardez l'autre main libre.",
        ["Sweep attack", "Double damage without chestplate", "Two-handed (I)"],
        ["Attaque en balayage", "Dégâts doublés sans plastron", "À deux mains (I)"]),
    "saber": (
        "Saber", "Sabre",
        "A cavalry sword with a sweeping strike.\\nHolding it absorbs part of the melee damage you take.",
        "Une épée de cavalerie à la frappe balayante.\\nLa tenir absorbe une partie des dégâts de mêlée reçus.",
        "The duellist's choice: a sweeping blade that also protects you. While held, it absorbs a quarter of incoming melee damage.",
        "Le choix des duellistes : une lame balayante qui vous protège aussi. Tenue en main, elle absorbe un quart des dégâts de mêlée.",
        ["Absorbs 25% melee damage", "Double damage without chestplate", "Sweep attack"],
        ["Absorbe 25 % des dégâts de mêlée", "Dégâts doublés sans plastron", "Attaque en balayage"]),
    "spear": (
        "Spear", "Lance",
        "A simple, reliable polearm that keeps enemies at arm's length.",
        "Une arme d'hast simple et fiable, qui tient les ennemis à distance.",
        "The oldest trick in the book: a long shaft and a sharp point. Strike your enemies before they get close enough to hit back.",
        "La plus vieille ruse du monde : un long manche et une pointe acérée. Frappez vos ennemis avant qu'ils ne soient assez près pour riposter.",
        ["Reach: 6 blocks"],
        ["Portée : 6 blocs"]),
    "mace": (
        "Flanged Mace", "Masse à ailettes",
        "A flanged club that cracks bones.\\nEspecially effective against the undead.",
        "Une masse à ailettes qui brise les os.\\nParticulièrement efficace contre les morts-vivants.",
        "Heavy flanges concentrate every blow. Zombies, skeletons and their kin take half again as much damage.",
        "De lourdes ailettes concentrent chaque coup. Zombies, squelettes et autres morts-vivants subissent moitié plus de dégâts.",
        ["+50% damage to undead"],
        ["+50 % de dégâts aux morts-vivants"]),
    "quarterstaff": (
        "Quarterstaff", "Bâton de combat",
        "A fast two-handed staff that sweeps around you.\\nThe head material caps the top end, the binding the bottom end.",
        "Un bâton rapide à deux mains qui balaie autour de vous.\\nLe matériau de la tête coiffe le haut, celui de l'attache le bas.",
        "Quick and versatile: each end can be capped with a different material. Its sweep deals half damage to everything nearby.",
        "Rapide et polyvalent : chaque extrémité peut être coiffée d'un matériau différent. Son balayage inflige la moitié des dégâts autour de vous.",
        ["Sweep: 50% damage", "Two-handed (I)", "Fast"],
        ["Balayage : 50 % des dégâts", "À deux mains (I)", "Rapide"]),
    "throwing_knife": (
        "Throwing Knife", "Couteau de lancer",
        "A stack of balanced knives: their number depends on the materials.\\nHold right-click and release to throw one.",
        "Une pile de couteaux équilibrés : leur nombre dépend des matériaux.\\nMaintenez clic droit puis relâchez pour en lancer un.",
        "Thrown knives deal double damage. Like Tinkers arrows, the number of knives comes from the durability of the materials: pick them back up to refill the stack.",
        "Lancés, les couteaux infligent le double de dégâts. Comme les flèches Tinkers, leur nombre vient de la durabilité des matériaux : ramassez-les pour recharger la pile.",
        ["Thrown: double damage", "Ammo from materials", "Fast"],
        ["Lancé : dégâts doublés", "Munitions selon matériaux", "Rapide"]),
    "throwing_axe": (
        "Throwing Axe", "Hache de lancer",
        "A stack of heavy throwing axes: their number depends on the materials.\\nHold right-click and release to throw one.",
        "Une pile de lourdes haches de lancer : leur nombre dépend des matériaux.\\nMaintenez clic droit puis relâchez pour en lancer une.",
        "Slower to throw than knives, but each axe hits hard and deals double damage. Pick them back up to refill the stack.",
        "Plus lentes à lancer que les couteaux, mais chaque hache frappe fort et inflige le double de dégâts. Ramassez-les pour recharger la pile.",
        ["Thrown: double damage", "Ammo from materials"],
        ["Lancée : dégâts doublés", "Munitions selon matériaux"]),
    "javelin": (
        "Javelin", "Javelot",
        "A bundle of javelins that deal heavy damage when thrown.\\nHold right-click and release to throw one.",
        "Un lot de javelots aux lourds dégâts en lancer.\\nMaintenez clic droit puis relâchez pour en lancer un.",
        "Charge the throw fully to send a javelin flying far and true: it deals triple damage. Fewer per bundle, but devastating.",
        "Chargez complètement le lancer pour envoyer le javelot loin et droit : il inflige le triple de dégâts. Moins nombreux, mais dévastateurs.",
        ["Thrown: triple damage", "Ammo from materials"],
        ["Lancé : dégâts triplés", "Munitions selon matériaux"]),
    "boomerang": (
        "Boomerang", "Boomerang",
        "Hold right-click and release to throw it:\\nit flies back to you after hitting something.",
        "Maintenez clic droit puis relâchez pour le lancer :\\nil vous revient après avoir touché quelque chose.",
        "Throw it at a target and catch it on the way back. The longer you charge, the further it flies before turning around.",
        "Lancez-le sur une cible et rattrapez-le au retour. Plus vous chargez, plus il va loin avant de faire demi-tour.",
        ["Returns to the thrower", "Charge to fly further"],
        ["Revient au lanceur", "Charger pour aller plus loin"]),
    "pike": (
        "Pike", "Pique",
        "A very long spear held with both hands.\\nHits from further away than any other weapon.",
        "Une très longue lance tenue à deux mains.\\nFrappe de plus loin que toute autre arme.",
        "The longest reach in the arsenal. Hold the line and strike from seven blocks away, but keep your other hand free.",
        "La plus grande allonge de l'arsenal. Tenez la ligne et frappez à sept blocs, mais gardez l'autre main libre.",
        ["Reach: 7 blocks", "Two-handed (I)"],
        ["Portée : 7 blocs", "À deux mains (I)"]),
    "lance": (
        "Lance", "Lance de joute",
        "A knightly weapon with extended reach.\\nDeals double damage while riding.",
        "Une arme de chevalier à la portée étendue.\\nInflige le double de dégâts à cheval.",
        "Made for the charge. Mounted on a horse, every hit deals double damage. The large plate protects the hand that holds it.",
        "Faite pour la charge. À cheval, chaque coup inflige le double de dégâts. La large plaque protège la main qui la tient.",
        ["Reach: 6 blocks", "Double damage while riding"],
        ["Portée : 6 blocs", "Dégâts doublés à cheval"]),
    "glaive": (
        "Glaive", "Glaive",
        "A bladed polearm with extended reach.\\nIts sweep deals half damage around the target.",
        "Une arme d'hast tranchante à la portée étendue.\\nSon balayage inflige la moitié des dégâts autour de la cible.",
        "A long blade on a long shaft: reach and sweep in one weapon. Wield it with both hands.",
        "Une longue lame sur un long manche : la portée et le balayage dans une seule arme. À manier à deux mains.",
        ["Reach: 6 blocks", "Sweep: 50% damage", "Two-handed (I)"],
        ["Portée : 6 blocs", "Balayage : 50 % des dégâts", "À deux mains (I)"]),
    "greatsword": (
        "Greatsword", "Espadon",
        "A massive two-handed sword.\\nIts sweep deals full damage to everything around the target.",
        "Une énorme épée à deux mains.\\nSon balayage inflige tous les dégâts autour de la cible.",
        "Slow, heavy and unstoppable. Every enemy caught in its sweep takes the full blow. Requires both hands.",
        "Lent, lourd et implacable. Chaque ennemi pris dans son balayage encaisse le coup entier. Exige les deux mains.",
        ["Sweep: full damage", "Reach: 6 blocks", "Two-handed (II)"],
        ["Balayage : dégâts complets", "Portée : 6 blocs", "À deux mains (II)"]),
    "halberd": (
        "Halberd", "Hallebarde",
        "A heavy polearm with extended reach.\\nIts blows disable shields.",
        "Une lourde arme d'hast à la portée étendue.\\nSes coups désactivent les boucliers.",
        "Axe, spike and hook on one long shaft. It hits from afar and knocks shields aside, leaving defenders open.",
        "Hache, pique et crochet sur un long manche. Elle frappe de loin et écarte les boucliers, laissant les défenseurs à découvert.",
        ["Reach: 6 blocks", "Disables shields", "Two-handed (II)"],
        ["Portée : 6 blocs", "Désactive les boucliers", "À deux mains (II)"]),
    "battle_axe": (
        "Battleaxe", "Hache de bataille",
        "A heavy double-bitted axe.\\nA weapon first, but it also chops wood.",
        "Une lourde hache à double tranchant.\\nUne arme avant tout, mais elle coupe aussi le bois.",
        "Built for war, handy in the forest: it mines wood like an axe, using the harvest level of its head.",
        "Faite pour la guerre, utile en forêt : elle abat le bois comme une hache, avec le niveau de minage de sa tête.",
        ["Also an axe", "Two-handed (I)"],
        ["Sert aussi de hache", "À deux mains (I)"]),
    "warhammer": (
        "Warhammer", "Marteau de guerre",
        "Its spike punches through plate.\\nHalf of the damage ignores armour.",
        "Sa pointe perce les plaques.\\nLa moitié des dégâts ignore l'armure.",
        "The answer to heavy armour: half of every blow goes straight through it. Wield it with both hands.",
        "La réponse aux armures lourdes : la moitié de chaque coup les traverse. À manier à deux mains.",
        ["Ignores 50% armour", "Two-handed (I)"],
        ["Ignore 50 % de l'armure", "À deux mains (I)"]),
    "battle_hammer": (
        "Battle Hammer", "Marteau de bataille",
        "A slow, crushing hammer.\\nKnocks enemies back and leaves them dazed.",
        "Un marteau lent et écrasant.\\nRepousse les ennemis et les laisse étourdis.",
        "Every hit sends enemies flying and leaves them reeling with nausea. Slow to swing, impossible to ignore.",
        "Chaque coup projette les ennemis et les laisse nauséeux. Lent à manier, impossible à ignorer.",
        ["Strong knockback", "Inflicts nausea"],
        ["Fort recul", "Inflige la nausée"]),
    "war_scythe": (
        "War Scythe", "Faux de guerre",
        "A reaping weapon with a wide sweep.\\nHits harder against foes without a helmet.",
        "Une arme de moisson au large balayage.\\nFrappe plus fort les ennemis sans casque.",
        "Its sweep reaches further than any blade. Enemies without a helmet take double damage. Requires both hands.",
        "Son balayage porte plus loin que toute lame. Les ennemis sans casque subissent le double de dégâts. Exige les deux mains.",
        ["Wide sweep", "Double damage without helmet", "Two-handed (II)"],
        ["Large balayage", "Dégâts doublés sans casque", "À deux mains (II)"]),
}
ORDER = ["dagger", "parrying_dagger", "katana", "saber", "spear", "mace", "quarterstaff", "throwing_knife",
         "throwing_axe", "javelin", "boomerang", "pike", "lance", "glaive", "greatsword", "halberd", "battle_axe",
         "warhammer", "battle_hammer", "war_scythe"]
FORGE = {"pike", "lance", "glaive", "greatsword", "halberd", "battle_axe", "warhammer", "battle_hammer", "war_scythe"}

# ------------------------------------------------------------------------- pages de texte du livre (EN, FR)
B, P = "bold", "paragraph"


def t(text, **style):
    d = {"text": text}
    d.update(style)
    return d


NL = t("\n")
TEXT_PAGES = {
    "intro": (
        ("Spartan Arsenal", [
            t("The weapons of Spartan Weaponry, forged the Tinkers way."), NL, NL,
            t("Every weapon is built from "), t("existing Tinkers parts", color="dark_blue"),
            t(" at the Tool Station or the Tool Forge: pick the weapon, then place the parts shown in the slots."), NL, NL,
            t("Materials, traits and modifiers work as on any Tinkers tool. On top of that, each weapon keeps its "),
            t("Spartan properties", color="dark_blue"), t(": reach, sweep, two-handed..."),
        ]),
        ("Arsenal spartiate", [
            t("Les armes de Spartan Weaponry, forgées à la manière de Tinkers."), NL, NL,
            t("Chaque arme se fabrique avec des "), t("pièces Tinkers existantes", color="dark_blue"),
            t(" à la Tool Station ou à la Tool Forge : choisissez l'arme, puis placez les pièces indiquées."), NL, NL,
            t("Matériaux, traits et modificateurs fonctionnent comme sur tout outil Tinkers. En plus, chaque arme garde ses "),
            t("propriétés Spartan", color="dark_blue"), t(" : portée, balayage, deux mains..."),
        ])),
    "properties": (
        ("Properties", [
            t("Reach", color="dark_blue"), t(": strike from 6 or 7 blocks away."), NL,
            t("Two-handed", color="dark_blue"), t(": with something in your other hand, damage drops by 50% (I) or 75% (II) and you suffer mining fatigue."), NL,
            t("Sweep", color="dark_blue"), t(": also hits enemies around the target, for 1 damage, half or full damage."), NL,
            t("Armour piercing", color="dark_blue"), t(": part of the damage ignores armour."), NL,
            t("Shield breach", color="dark_blue"), t(": disables the target's shield."),
        ]),
        ("Propriétés", [
            t("Portée", color="dark_blue"), t(" : frappe à 6 ou 7 blocs."), NL,
            t("À deux mains", color="dark_blue"), t(" : avec un objet dans l'autre main, les dégâts baissent de 50 % (I) ou 75 % (II) et vous subissez la fatigue."), NL,
            t("Balayage", color="dark_blue"), t(" : touche aussi les ennemis autour de la cible, pour 1 dégât, la moitié ou la totalité."), NL,
            t("Perforation", color="dark_blue"), t(" : une partie des dégâts ignore l'armure."), NL,
            t("Brise-bouclier", color="dark_blue"), t(" : désactive le bouclier de la cible."),
        ])),
    "properties2": (
        ("Properties (2)", [
            t("Bonus damage", color="dark_blue"), t(": extra damage in specific cases: from behind, against undead, while riding, or against enemies missing a chestplate or helmet."), NL,
            t("Damage absorb", color="dark_blue"), t(": while held, reduces the melee damage you take."), NL,
            t("Parry", color="dark_blue"), t(": hold right-click to turn aside melee attacks."), NL,
            t("Versatile", color="dark_blue"), t(": also works as an axe."), NL, NL,
            t("Hold "), t("Shift", color="dark_blue"), t(" over a weapon to read the details of its properties."),
        ]),
        ("Propriétés (2)", [
            t("Dégâts bonus", color="dark_blue"), t(" : dégâts en plus dans certains cas : dans le dos, contre les morts-vivants, à cheval, ou contre les ennemis sans plastron ou sans casque."), NL,
            t("Absorption", color="dark_blue"), t(" : tenue en main, réduit les dégâts de mêlée reçus."), NL,
            t("Parade", color="dark_blue"), t(" : maintenez clic droit pour détourner les attaques de mêlée."), NL,
            t("Polyvalente", color="dark_blue"), t(" : sert aussi de hache."), NL, NL,
            t("Maintenez "), t("Maj", color="dark_blue"), t(" sur une arme pour lire le détail de ses propriétés."),
        ])),
    "throwing": (
        ("Throwing", [
            t("Hold right-click, then release to throw. The longer you charge, the faster and harder the throw."), NL, NL,
            t("Throwing knives, axes and javelins come in stacks. Like Tinkers arrows, "),
            t("the number of projectiles comes from the durability of the materials", color="dark_blue"),
            t(": better materials, more throws."), NL, NL,
            t("Pick your projectiles back up: they return to an identical stack. Each hit wears them down, and an empty stack stays in your inventory until you refill it."),
        ]),
        ("Lancer", [
            t("Maintenez clic droit, puis relâchez pour lancer. Plus vous chargez, plus le lancer est rapide et puissant."), NL, NL,
            t("Couteaux, haches de lancer et javelots se présentent en piles. Comme pour les flèches Tinkers, "),
            t("le nombre de projectiles vient de la durabilité des matériaux", color="dark_blue"),
            t(" : meilleurs matériaux, plus de lancers."), NL, NL,
            t("Ramassez vos projectiles : ils retournent dans une pile identique. Chaque coup les use, et une pile vide reste dans l'inventaire jusqu'à ce que vous la rechargiez."),
        ])),
    "smeltery": (
        ("Smeltery", [
            t("Spartan Weaponry's own metal weapons can be melted in the smeltery to recover their metal."), NL, NL,
            t("The amount follows the crafting recipe, and "), t("worn weapons give back less", color="dark_blue"),
            t(": a half-broken iron sword returns about half of its iron."), NL, NL,
            t("The Tinkers weapons of this arsenal work like any Tinkers tool: their parts can be cast and melted as usual."),
        ]),
        ("Smeltery", [
            t("Les armes en métal de Spartan Weaponry peuvent être fondues dans la smeltery pour récupérer leur métal."), NL, NL,
            t("La quantité suit la recette de fabrication, et "), t("une arme usée rend moins", color="dark_blue"),
            t(" : une épée en fer à moitié usée rend environ la moitié de son fer."), NL, NL,
            t("Les armes Tinkers de cet arsenal fonctionnent comme tout outil Tinkers : leurs pièces se coulent et se fondent normalement."),
        ])),
}
TEXT_ORDER = ["intro", "properties", "properties2", "throwing", "smeltery"]


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    book = ROOT / "book"
    # index : une section « spartan » dans le livre Tinkers
    write(book / "index.json", [{
        "name": "spartan",
        "data": "sections/spartan.json",
        "icon": {"item": {"id": "spartanweaponry:katana_iron"}},
    }])
    pages = []
    for name in TEXT_ORDER:
        pages.append({"name": "hidden" if name == "properties2" else "spartan_" + name, "type": "text",
                      "data": "spartan/" + name + ".json"})
    for w in ORDER:
        pages.append({"name": w, "type": "tool", "data": "spartan/" + w + ".json"})
    write(book / "sections" / "spartan.json", pages)

    for lang, i in (("en_us", 0), ("fr_fr", 1)):
        for name, texts in TEXT_PAGES.items():
            title, body = texts[i]
            write(book / lang / "spartan" / (name + ".json"), {"title": title, "text": body})
        for w in ORDER:
            row = W[w]
            write(book / lang / "spartan" / (w + ".json"), {
                "tool": w,
                "text": [t(row[4 + i])],
                "properties": row[6 + i] + (["Tool Forge"] if w in FORGE else ["Tool Station"]),
            })
        names = {"spartan": ("Spartan Arsenal", "Arsenal spartiate")[i],
                 "spartan_intro": ("Introduction", "Introduction")[i],
                 "spartan_properties": ("Properties", "Propriétés")[i],
                 "spartan_throwing": ("Throwing and ammo", "Lancer et munitions")[i],
                 "spartan_smeltery": ("Smeltery", "Smeltery")[i]}
        (book / lang / "language.lang").write_text(
            "".join(f"{k}={v}\n" for k, v in names.items()), encoding="utf-8")

    # fichiers de langue du jeu : noms et descriptions
    for lang, i in (("en_us", 0), ("fr_fr", 1)):
        path = ROOT / "lang" / (lang + ".lang")
        keep = [line for line in path.read_text(encoding="utf-8").splitlines()
                if not line.startswith("item.tconspartan.") and not line.startswith("# Tool")]
        out = ["# " + ("Weapons" if i == 0 else "Armes")]
        for w in ORDER:
            row = W[w]
            out.append(f"item.tconspartan.{w}.name={row[i]}")
            out.append(f"item.tconspartan.{w}.desc={row[2 + i]}")
        rest = "\n".join(keep).strip("\n")
        path.write_text("\n".join(out) + "\n\n" + rest + "\n", encoding="utf-8")
    print("ok")


if __name__ == "__main__":
    main()
