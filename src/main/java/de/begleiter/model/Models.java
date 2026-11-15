package de.begleiter.model;

import static de.begleiter.model.CompanionModel.Slot.DARK;
import static de.begleiter.model.CompanionModel.Slot.HAIR;
import static de.begleiter.model.CompanionModel.Slot.HAIR_SIDE;
import static de.begleiter.model.CompanionModel.Slot.INNER;
import static de.begleiter.model.CompanionModel.Slot.PANTS;
import static de.begleiter.model.CompanionModel.Slot.PRIMARY;
import static de.begleiter.model.CompanionModel.Slot.SECONDARY;
import static de.begleiter.model.CompanionModel.Slot.SHIRT;
import static de.begleiter.model.CompanionModel.Slot.SHOE;
import static de.begleiter.model.CompanionModel.Slot.SKIN;
import static de.begleiter.model.Group.ARM_L;
import static de.begleiter.model.Group.ARM_R;
import static de.begleiter.model.Group.BODY;
import static de.begleiter.model.Group.EAR_L;
import static de.begleiter.model.Group.EAR_R;
import static de.begleiter.model.Group.HEAD;
import static de.begleiter.model.Group.LEG_BL;
import static de.begleiter.model.Group.LEG_BR;
import static de.begleiter.model.Group.LEG_FL;
import static de.begleiter.model.Group.LEG_FR;
import static de.begleiter.model.Group.TAIL;

import java.util.EnumMap;
import java.util.Map;

/** Die vier eingebauten Tiermodelle. Alle Maße in Pixeln (16 Pixel = 1 Block). */
public final class Models {
    private Models() {}

    private static final Map<AnimalType, CompanionModel> CACHE = new EnumMap<>(AnimalType.class);

    public static synchronized CompanionModel of(AnimalType type) {
        return CACHE.computeIfAbsent(type, Models::build);
    }

    private static CompanionModel build(AnimalType type) {
        return switch (type) {
            case CAT -> cat();
            case DOG -> dog();
            case RABBIT -> rabbit();
            case FOX -> fox();
            case HUMAN -> human();
        };
    }

    private static CompanionModel cat() {
        return CompanionModel.builder(AnimalType.CAT)
            .shape(5.5f, 13f, 4f, 3f, 0.34f, 0.7f)
            .pivot(BODY, 0, 4, -5.5f)
            .pivot(HEAD, 0, 8, 4.5f)
            .pivot(TAIL, 0, 7.5f, -5.5f)
            .pivot(EAR_L, 2f, 12.3f, 5.6f)
            .pivot(EAR_R, -2f, 12.3f, 5.6f)
            .pivot(LEG_FL, 2.1f, 4, 3.1f)
            .pivot(LEG_FR, -2.1f, 4, 3.1f)
            .pivot(LEG_BL, 2.1f, 4, -3.5f)
            .pivot(LEG_BR, -2.1f, 4, -3.5f)
            // Körper
            .box(BODY, PRIMARY, -2.5f, 4, -5.5f, 5, 4.5f, 11)
            .box(BODY, SECONDARY, -1.6f, 4.4f, 5.4f, 3.2f, 3, 0.4f)
            // Kopf
            .box(HEAD, PRIMARY, -3, 7.5f, 4, 6, 5, 5)
            .box(HEAD, SECONDARY, -1.6f, 8, 8.9f, 3.2f, 2, 1)
            .box(HEAD, INNER, -0.6f, 9.1f, 9.8f, 1.2f, 0.8f, 0.4f)
            .mirrored(HEAD, HEAD, DARK, 1.1f, 10.2f, 8.9f, 1, 1.2f, 0.4f)
            // Ohren
            .box(EAR_L, PRIMARY, 1, 12.3f, 5, 2, 2.2f, 1.2f)
            .box(EAR_L, INNER, 1.4f, 12.4f, 6.1f, 1.2f, 1.4f, 0.3f)
            .box(EAR_R, PRIMARY, -3, 12.3f, 5, 2, 2.2f, 1.2f)
            .box(EAR_R, INNER, -2.6f, 12.4f, 6.1f, 1.2f, 1.4f, 0.3f)
            // Schwanz
            .box(TAIL, PRIMARY, -0.8f, 6.8f, -14, 1.6f, 1.6f, 8.5f)
            .box(TAIL, SECONDARY, -0.85f, 6.75f, -14.4f, 1.7f, 1.7f, 2)
            // Beine
            .box(LEG_FL, PRIMARY, 1.2f, 1, 2.2f, 1.8f, 3.1f, 1.8f)
            .box(LEG_FL, SECONDARY, 1.2f, 0, 2.2f, 1.8f, 1, 1.9f)
            .box(LEG_FR, PRIMARY, -3, 1, 2.2f, 1.8f, 3.1f, 1.8f)
            .box(LEG_FR, SECONDARY, -3, 0, 2.2f, 1.8f, 1, 1.9f)
            .box(LEG_BL, PRIMARY, 1.2f, 1, -4.4f, 1.8f, 3.1f, 1.8f)
            .box(LEG_BL, SECONDARY, 1.2f, 0, -4.4f, 1.8f, 1, 1.9f)
            .box(LEG_BR, PRIMARY, -3, 1, -4.4f, 1.8f, 3.1f, 1.8f)
            .box(LEG_BR, SECONDARY, -3, 0, -4.4f, 1.8f, 1, 1.9f)
            .build();
    }

    private static CompanionModel dog() {
        return CompanionModel.builder(AnimalType.DOG)
            .shape(6.5f, 14f, 5f, 3.2f, 0.32f, 0.5f)
            .pivot(BODY, 0, 5, -6)
            .pivot(HEAD, 0, 9, 5.5f)
            .pivot(TAIL, 0, 9.5f, -6)
            .pivot(EAR_L, 3.2f, 13.8f, 7.3f)
            .pivot(EAR_R, -3.2f, 13.8f, 7.3f)
            .pivot(LEG_FL, 2.1f, 5, 4.2f)
            .pivot(LEG_FR, -2.1f, 5, 4.2f)
            .pivot(LEG_BL, 2.1f, 5, -4.4f)
            .pivot(LEG_BR, -2.1f, 5, -4.4f)
            // Körper
            .box(BODY, PRIMARY, -2.8f, 5, -6, 5.6f, 5, 12)
            .box(BODY, SECONDARY, -1.8f, 5.6f, 5.9f, 3.6f, 3.6f, 0.4f)
            // Kopf mit Schnauze
            .box(HEAD, PRIMARY, -2.7f, 8.5f, 5, 5.4f, 5, 5)
            .box(HEAD, SECONDARY, -1.7f, 8.6f, 9.9f, 3.4f, 2.6f, 3)
            .box(HEAD, DARK, -0.9f, 10.4f, 12.7f, 1.8f, 1.2f, 0.5f)
            .mirrored(HEAD, HEAD, DARK, 1.2f, 11.6f, 9.9f, 1, 1.2f, 0.4f)
            // Hängeohren
            .box(EAR_L, PRIMARY, 2.7f, 10.5f, 6.2f, 1, 3.3f, 2.2f)
            .box(EAR_R, PRIMARY, -3.7f, 10.5f, 6.2f, 1, 3.3f, 2.2f)
            // Schwanz
            .box(TAIL, PRIMARY, -0.7f, 9, -11, 1.4f, 1.4f, 5)
            .box(TAIL, SECONDARY, -0.75f, 8.95f, -11.4f, 1.5f, 1.5f, 1.6f)
            // Beine
            .box(LEG_FL, PRIMARY, 1.1f, 1.2f, 3.2f, 2, 4, 2)
            .box(LEG_FL, SECONDARY, 1.1f, 0, 3.2f, 2, 1.2f, 2.2f)
            .box(LEG_FR, PRIMARY, -3.1f, 1.2f, 3.2f, 2, 4, 2)
            .box(LEG_FR, SECONDARY, -3.1f, 0, 3.2f, 2, 1.2f, 2.2f)
            .box(LEG_BL, PRIMARY, 1.1f, 1.2f, -5.4f, 2, 4, 2)
            .box(LEG_BL, SECONDARY, 1.1f, 0, -5.4f, 2, 1.2f, 2.2f)
            .box(LEG_BR, PRIMARY, -3.1f, 1.2f, -5.4f, 2, 4, 2)
            .box(LEG_BR, SECONDARY, -3.1f, 0, -5.4f, 2, 1.2f, 2.2f)
            .build();
    }

    private static CompanionModel rabbit() {
        return CompanionModel.builder(AnimalType.RABBIT)
            .shape(5f, 11f, 3.5f, 1.6f, 0.45f, 0f)
            .pivot(BODY, 0, 3.5f, -4)
            .pivot(HEAD, 0, 7, 4)
            .pivot(TAIL, 0, 5, -4)
            .pivot(EAR_L, 1.2f, 10.8f, 5)
            .pivot(EAR_R, -1.2f, 10.8f, 5)
            .pivot(LEG_FL, 1.8f, 3.5f, 3.2f)
            .pivot(LEG_FR, -1.8f, 3.5f, 3.2f)
            .pivot(LEG_BL, 2.7f, 4, -2.8f)
            .pivot(LEG_BR, -2.7f, 4, -2.8f)
            // Körper
            .box(BODY, PRIMARY, -2.5f, 3.5f, -4, 5, 4.5f, 8)
            .box(BODY, SECONDARY, -1.6f, 3.8f, 3.9f, 3.2f, 3, 0.4f)
            // Kopf
            .box(HEAD, PRIMARY, -2.5f, 6.5f, 3.5f, 5, 4.5f, 4.5f)
            .box(HEAD, SECONDARY, -1.4f, 6.7f, 7.9f, 2.8f, 2, 0.8f)
            .box(HEAD, INNER, -0.5f, 8, 8.6f, 1, 0.8f, 0.4f)
            .mirrored(HEAD, HEAD, DARK, 1.2f, 9, 7.8f, 0.9f, 1.1f, 0.4f)
            // lange Ohren
            .box(EAR_L, PRIMARY, 0.4f, 10.8f, 4.4f, 1.6f, 6, 1)
            .box(EAR_L, INNER, 0.7f, 11, 5.4f, 0.9f, 5, 0.3f)
            .box(EAR_R, PRIMARY, -2, 10.8f, 4.4f, 1.6f, 6, 1)
            .box(EAR_R, INNER, -1.6f, 11, 5.4f, 0.9f, 5, 0.3f)
            // Puschelschwanz
            .box(TAIL, SECONDARY, -1.2f, 4.2f, -6.4f, 2.4f, 2.4f, 2.4f)
            // Vorderbeine (dünn) und Hinterkeulen (kräftig)
            .box(LEG_FL, PRIMARY, 1, 0.5f, 2.5f, 1.5f, 3.2f, 1.5f)
            .box(LEG_FL, SECONDARY, 1, 0, 2.5f, 1.5f, 1, 1.8f)
            .box(LEG_FR, PRIMARY, -2.5f, 0.5f, 2.5f, 1.5f, 3.2f, 1.5f)
            .box(LEG_FR, SECONDARY, -2.5f, 0, 2.5f, 1.5f, 1, 1.8f)
            .box(LEG_BL, PRIMARY, 1.5f, 0.2f, -4.8f, 2.5f, 4.2f, 4)
            .box(LEG_BL, SECONDARY, 1.5f, 0, -2f, 2.5f, 0.8f, 2.2f)
            .box(LEG_BR, PRIMARY, -4, 0.2f, -4.8f, 2.5f, 4.2f, 4)
            .box(LEG_BR, SECONDARY, -4, 0, -2f, 2.5f, 0.8f, 2.2f)
            .build();
    }

    private static CompanionModel fox() {
        return CompanionModel.builder(AnimalType.FOX)
            .shape(6.5f, 13f, 5f, 3.2f, 0.32f, 0.15f)
            .pivot(BODY, 0, 5, -6)
            .pivot(HEAD, 0, 9, 5)
            .pivot(TAIL, 0, 9, -6)
            .pivot(EAR_L, 2f, 12.9f, 6.2f)
            .pivot(EAR_R, -2f, 12.9f, 6.2f)
            .pivot(LEG_FL, 1.9f, 5, 4.1f)
            .pivot(LEG_FR, -1.9f, 5, 4.1f)
            .pivot(LEG_BL, 1.9f, 5, -4.3f)
            .pivot(LEG_BR, -1.9f, 5, -4.3f)
            // Körper
            .box(BODY, PRIMARY, -2.5f, 5, -6, 5, 4.5f, 11.5f)
            .box(BODY, SECONDARY, -1.7f, 5.2f, 5.4f, 3.4f, 3.8f, 0.4f)
            // Kopf mit spitzer Schnauze
            .box(HEAD, PRIMARY, -2.8f, 8.5f, 4.5f, 5.6f, 4.6f, 4.6f)
            .box(HEAD, SECONDARY, -1.2f, 8.7f, 9, 2.4f, 2, 3)
            .box(HEAD, DARK, -0.7f, 9.8f, 11.8f, 1.4f, 1, 0.5f)
            .mirrored(HEAD, HEAD, DARK, 1.5f, 10.8f, 8.9f, 1, 1, 0.4f)
            // spitze Ohren mit dunkler Innenseite
            .box(EAR_L, PRIMARY, 1.1f, 12.9f, 5.5f, 2, 3.4f, 1.2f)
            .box(EAR_L, DARK, 1.5f, 12.9f, 6.6f, 1.2f, 2.4f, 0.3f)
            .box(EAR_R, PRIMARY, -3.1f, 12.9f, 5.5f, 2, 3.4f, 1.2f)
            .box(EAR_R, DARK, -2.7f, 12.9f, 6.6f, 1.2f, 2.4f, 0.3f)
            // buschiger Schwanz mit weißer Spitze
            .box(TAIL, PRIMARY, -1.6f, 7.6f, -15, 3.2f, 3.2f, 9.3f)
            .box(TAIL, SECONDARY, -1.7f, 7.5f, -16.5f, 3.4f, 3.4f, 2.6f)
            // Beine mit dunklen "Socken"
            .box(LEG_FL, PRIMARY, 1, 1.6f, 3.2f, 1.8f, 3.6f, 1.8f)
            .box(LEG_FL, DARK, 1, 0, 3.2f, 1.8f, 1.6f, 1.9f)
            .box(LEG_FR, PRIMARY, -2.8f, 1.6f, 3.2f, 1.8f, 3.6f, 1.8f)
            .box(LEG_FR, DARK, -2.8f, 0, 3.2f, 1.8f, 1.6f, 1.9f)
            .box(LEG_BL, PRIMARY, 1, 1.6f, -5.2f, 1.8f, 3.6f, 1.8f)
            .box(LEG_BL, DARK, 1, 0, -5.2f, 1.8f, 1.6f, 1.9f)
            .box(LEG_BR, PRIMARY, -2.8f, 1.6f, -5.2f, 1.8f, 3.6f, 1.8f)
            .box(LEG_BR, DARK, -2.8f, 0, -5.2f, 1.8f, 1.6f, 1.9f)
            .build();
    }

    /** Kleiner Mensch (halb so groß wie Steve): Kopf 4, Rumpf 6, Beine 6 Pixel hoch. */
    private static CompanionModel human() {
        return CompanionModel.builder(AnimalType.HUMAN)
            .shape(4.5f, 17f, 6f, 3f, 0f, 0f)
            .pivot(BODY, 0, 6, 0)
            .pivot(HEAD, 0, 12, 0)
            .pivot(ARM_L, 3, 11.5f, 0)
            .pivot(ARM_R, -3, 11.5f, 0)
            .pivot(LEG_FL, 1, 6, 0)
            .pivot(LEG_FR, -1, 6, 0)
            // Rumpf
            .box(BODY, SHIRT, -2, 6, -1, 4, 6, 2)
            // Kopf mit Haaren (oben, hinten, Pony) und Augen
            .box(HEAD, SKIN, -2, 12, -2, 4, 4, 4)
            .box(HEAD, HAIR, -2.1f, 15.2f, -2.1f, 4.2f, 1, 4.2f)
            .box(HEAD, HAIR_SIDE, -2.1f, 12, -2.1f, 4.2f, 3.3f, 1)
            .mirrored(HEAD, HEAD, HAIR_SIDE, 1.9f, 13.3f, -2.1f, 0.3f, 2.2f, 3.8f)
            .box(HEAD, HAIR, -2.05f, 14.3f, 1.7f, 4.1f, 0.9f, 0.4f)
            .mirrored(HEAD, HEAD, DARK, 0.6f, 13.3f, 2f, 0.9f, 0.9f, 0.25f)
            // Arme: Ärmel und Hand
            .box(ARM_L, SHIRT, 2, 8, -1, 2, 4, 2)
            .box(ARM_L, SKIN, 2, 6, -1, 2, 2, 2)
            .box(ARM_R, SHIRT, -4, 8, -1, 2, 4, 2)
            .box(ARM_R, SKIN, -4, 6, -1, 2, 2, 2)
            // Beine: Hose und Schuhe
            .box(LEG_FL, PANTS, 0, 1.5f, -1, 2, 4.5f, 2)
            .box(LEG_FL, SHOE, 0, 0, -1, 2, 1.5f, 2.3f)
            .box(LEG_FR, PANTS, -2, 1.5f, -1, 2, 4.5f, 2)
            .box(LEG_FR, SHOE, -2, 0, -1, 2, 1.5f, 2.3f)
            .build();
    }
}
