package de.begleiter.client;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Ein Spieler-Modell, das nur im Client existiert (wie ein anderer Spieler auf dem Server, aber ohne
 * Verbindung zu irgendetwas). Es kann nicht angeklickt, geschubst oder getroffen werden und hat keinen
 * Namen über dem Kopf. Die Skin-Textur wird von außen gesetzt.
 */
final class CompanionAvatar extends RemotePlayer {
    private PlayerSkin customSkin;

    CompanionAvatar(ClientLevel level) {
        super(level, new GameProfile(UUID.randomUUID(), "Begleiter"));
    }

    void setCustomSkin(PlayerSkin skin) {
        this.customSkin = skin;
    }

    @Override
    public PlayerSkin getSkin() {
        return customSkin != null ? customSkin : super.getSkin();
    }

    /** Schiebt weder den Spieler noch andere Entitäten. */
    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Das Fadenkreuz geht durch ihn hindurch, damit nie ein Angriff mit einer ungültigen ID gesendet wird. */
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }
}
