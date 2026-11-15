package de.begleiter;

import de.begleiter.client.CompanionManager;
import de.begleiter.config.BegleiterConfig;
import net.fabricmc.api.ClientModInitializer;

/** Einstiegspunkt. Der Mod läuft nur auf dem Client und sendet nichts an Server. */
public class BegleiterMod implements ClientModInitializer {
    public static final String MOD_ID = "begleiter";

    @Override
    public void onInitializeClient() {
        BegleiterConfig.load();
        CompanionManager.init();
    }
}
