package de.begleiter.mixin;

import de.begleiter.client.gui.CustomizeScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fügt dem Hauptmenü die Schaltfläche "Begleiter" hinzu (oben links). */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void begleiter$addButton(CallbackInfo ci) {
        this.addRenderableWidget(CustomizeScreen.menuButton((Screen) (Object) this, 6, 6, 100));
    }
}
