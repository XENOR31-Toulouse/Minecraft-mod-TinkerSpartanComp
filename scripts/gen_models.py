"""Génère les modèles Tinkers des armes de l'addon.

Usage :  python scripts/gen_models.py <chemin/vers/SpartanWeaponry-1.12.2-x.jar>

  - models/item/tools/<arme>.tcon.json : calques manche / tête / [garde-attache] (sprites générés en jeu, voir
    SpartanTextures.java) + transformations d'affichage reprises du modèle Spartan, pour que les grandes armes
    soient tenues comme dans Spartan ;
  - models/item/modifiers/<modificateur>.json : texture d'incrustation de chaque modificateur pour chaque arme.

À relancer après avoir ajouté une arme dans SpartanWeapon.java (garder WEAPONS synchronisé).
"""
import json
import pathlib
import shutil
import sys
import zipfile

MODID = "tconspartan"

# id de l'outil -> (nom de l'arme dans Spartan, a une 3e pièce garde/attache ?)  -- voir SpartanWeapon.java
WEAPONS = {
    "dagger": ("dagger", True),
    "parrying_dagger": ("parrying_dagger", True),
    "katana": ("katana", True),
    "saber": ("saber", True),
    "spear": ("spear", True),
    "mace": ("mace", True),
    "quarterstaff": ("quarterstaff", True),
    "throwing_knife": ("throwing_knife", False),
    "throwing_axe": ("throwing_axe", False),
    "javelin": ("javelin", False),
    "boomerang": ("boomerang", False),
    "pike": ("pike", True),
    "lance": ("lance", True),
    "glaive": ("glaive", True),
    "greatsword": ("greatsword", True),
    "halberd": ("halberd", True),
    "battle_axe": ("battleaxe", True),
    "warhammer": ("warhammer", True),
    "battle_hammer": ("hammer", True),
    "war_scythe": ("scythe", True),
}

# Armes dont les modèles Spartan portent un autre nom que leurs textures "custom"
MODEL_ALIASES = {"quarterstaff": "staff"}

# Doit correspondre à SpartanTextures.MODIFIERS
MODIFIERS = [
    "reinforced", "smite", "diamond", "blasting", "luck", "shulking", "bane_of_arthopods", "knockback",
    "beheading", "emerald", "silktouch", "mending_moss", "necrotic", "haste", "soulbound", "fiery",
    "sharpness", "glowing", "fortify", "webbed", "fins",
]

# Valeurs vanilla de item/generated + item/handheld (1.12.2), complétées par celles de Spartan.
DEFAULT_DISPLAY = {
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}


def spartan_display(jar: zipfile.ZipFile, weapon: str) -> dict:
    """Suit la chaîne de 'parent' du modèle Spartan de l'arme et fusionne les blocs 'display'."""
    names = jar.namelist()
    weapon = MODEL_ALIASES.get(weapon, weapon)
    start = next((c for c in (f"{weapon}_wood", f"{weapon}_iron", weapon)
                  if f"assets/spartanweaponry/models/item/{c}.json" in names), None)
    display, seen, name = {}, set(), start
    while name and name not in seen:
        seen.add(name)
        path = f"assets/spartanweaponry/models/item/{name}.json"
        if path not in names:
            break
        model = json.loads(jar.read(path).decode("utf-8"))
        for key, value in model.get("display", {}).items():
            display.setdefault(key, value)
        parent = model.get("parent", "")
        name = parent.split("/")[-1] if parent.startswith("spartanweaponry:") else None
    return display


def write(path: pathlib.Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=4) + "\n", encoding="utf-8")


def main() -> None:
    jar = zipfile.ZipFile(sys.argv[1])
    root = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets" / MODID / "models/item"
    shutil.rmtree(root / "parts", ignore_errors=True)  # anciennes pièces propres à l'addon (supprimées)

    for tool, (spartan, has_extra) in WEAPONS.items():
        display = dict(DEFAULT_DISPLAY)
        spartan_part = spartan_display(jar, spartan)
        display.update(spartan_part)
        textures = {
            "layer0": f"{MODID}:items/{tool}/handle",
            "layer1": f"{MODID}:items/{tool}/head",
        }
        if has_extra:
            textures["layer2"] = f"{MODID}:items/{tool}/extra"
        write(root / "tools" / f"{tool}.tcon.json", {"textures": textures, "display": display})
        print(f"{tool:16s} <- {spartan:16s} {len(textures)} calques, {len(spartan_part)} transforms Spartan")

    for modifier in MODIFIERS:
        textures = {tool: f"{MODID}:items/{tool}/mod_{modifier}" for tool in WEAPONS}
        write(root / "modifiers" / f"{modifier}.json", {"textures": textures})
    print(f"{len(MODIFIERS)} fichiers de modificateurs")


if __name__ == "__main__":
    main()
