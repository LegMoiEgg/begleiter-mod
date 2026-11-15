# Begleiter – kleiner optischer Mod-Begleiter (Fabric, Minecraft 26.3)

Ein Begleiter (Katze, Hund, Hase, Fuchs), der um dich herumläuft, sich selbst beschäftigt, wenn du stehst
oder AFK bist, und den du im Hauptmenü anpassen kannst. **Rein clientseitig und rein optisch**: nichts
wird an den Server gesendet, nichts in der Welt verändert, es gibt keinen Spielvorteil. Deshalb läuft er
auch auf Servern.

## Bedienung

- **Anpassen:** Schaltfläche „Begleiter“ oben links im Hauptmenü (und im Pausenmenü). Dort Tier, Fellfarbe,
  Akzentfarbe (Schnauze, Pfoten, Schwanzspitze), Größe und An/Aus. Die Vorschau zeigt verschiedene Posen.
- **Streicheln:** Rechtsklick mit **leerer Hand** auf den Begleiter. Er hüpft, wedelt und es erscheinen Herzchen.
  Dreimal schnell streicheln: er dreht sich.
- **Platz / Kopf / Komm:** Schleichen + Rechtsklick mit leerer Hand auf den Begleiter. Reihenfolge: Platz
  (bleibt sitzen), dann Kopf (sitzt auf deinem Kopf), dann Komm (läuft wieder frei). Vom Kopf holst du ihn
  herunter, indem du schleichst, steil nach oben schaust und Rechtsklick machst.
- **Kleiner Mensch:** Im Menü „Mensch“ wählen. Die Skin-PNG (64x64) als `begleiter-skin.png` in den `config`-Ordner deines Profils legen und im Menü „Skin laden“ klicken (Ziehen ins Fenster geht, falls es die Version unterstützt). Ein echter Skin
  lässt sich auf Block-Quader nicht als Textur legen, deshalb werden Hautfarbe, Haare, Oberteil, Hose und
  Schuhe aus der Datei als Blockfarben übernommen.
- **Teleport:** Nur wenn du festen Boden unter den Füßen hast (nicht beim Klettern, Fallen oder Elytra-Fliegen).
  Im Wasser schwimmt er hinterher.
- **Selbstbeschäftigung:** Wenn du ein paar Sekunden stehst, sitzt er, schaut sich um, putzt sich, jagt seinem
  Schwanz hinterher, spielt oder schnuppert (je nach Tier). Bist du lange AFK, schläft er ein und wacht auf,
  sobald du dich bewegst oder umschaust.
- Einstellungen liegen in `config/begleiter.json`.

## Bauen

Du brauchst Java 25 und Gradle 9.6. Am einfachsten ohne lokale Einrichtung: den Ordner als GitHub-Repository
hochladen. Der Workflow `.github/workflows/build.yml` baut die `.jar` (unter *Actions → Artefakte*).
Lokal: `gradle build`, die Datei liegt dann in `build/libs/begleiter-1.0.0.jar`.

## Im Norisk Launcher hinzufügen

1. Ein Profil mit **Fabric** und der Minecraft-Version, für die der Mod gebaut ist (Standard: **26.3**).
2. Im Profil **Fabric API** für dieselbe Version installieren (falls nicht schon enthalten).
3. Die Datei `begleiter-1.0.0.jar` zu den Mods des Profils hinzufügen (Mod-Ordner des Profils oder über
   die Mod-Verwaltung des Launchers).

## Andere Minecraft-Version

Der Mod nutzt bewusst keine eigene Render-Pipeline, sondern normale Block-Display-Entities nur im Client.
Dadurch ist er kaum an Render-Änderungen einzelner Versionen gebunden. Für eine andere Version ändern:
`gradle.properties` (`minecraft_version`, `fabric_api_version`, ggf. `loader_version`) und in
`src/main/resources/fabric.mod.json` den Bereich bei `"minecraft"`. Aktuelle Werte: <https://fabricmc.net/develop>

## Falls der Build Fehler meldet

Der Code wurde ohne Zugriff auf die Minecraft-26.3-Bibliotheken geschrieben. Die Modelle, Animationen,
das Verhalten und die Vorschau-Projektion sind getestet, die Anbindung an Minecraft nicht. Falls Namen
nicht stimmen, betrifft das mit hoher Wahrscheinlichkeit diese Stellen:

| Datei | Verdächtige Namen |
|---|---|
| `mixin/DisplayAccessor`, `mixin/BlockDisplayAccessor` | `setTransformation`, `setBlockState` (Display-Entities) |
| `client/CompanionRenderer` | `ClientLevel#addEntity`, `Entity#setId`, `Transformation`-Konstruktor |
| `client/CompanionManager` | `UseItemCallback`/`UseBlockCallback`-Signatur, `SoundEvents.*`, `Minecraft#isPaused` |
| `client/gui/CustomizeScreen` | `extractRenderState`, `GuiGraphicsExtractor#text/fill`, `Font#width` |
| `mixin/TitleScreenMixin`, `mixin/PauseScreenMixin` | Methodenname `init` |

Die Fehlermeldung des Builds genügt, um das gezielt zu korrigieren.

## Aufbau

- `model/` Tiermodelle aus Quadern, Posen, Animation, Vorschau-Projektion (ohne Minecraft-Abhängigkeit)
- `brain/` Verhalten und Bewegung (ohne Minecraft-Abhängigkeit)
- `client/` Darstellung in der Welt, Interaktion
- `client/gui/` Anpassungsmenü
- `mixin/` Schaltflächen im Menü, Zugriff auf Display-Entities

Neues Tier: in `AnimalType` eintragen und in `Models` ein Modell bauen.
