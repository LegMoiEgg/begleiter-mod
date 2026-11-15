package de.begleiter.mixin;

import de.begleiter.client.gui.CustomizeScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fügt dem Pausenmenü dieselbe Schaltfläche hinzu, damit man den Begleiter auch im Spiel ändern kann. */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void begleiter$addButton(CallbackInfo ci) {
        this.addRenderableWidget(CustomizeScreen.menuButton((Screen) (Object) this, 6, 6, 100));
    }
}
