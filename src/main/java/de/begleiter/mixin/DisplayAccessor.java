package de.begleiter.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Macht den (privaten) Transformations-Setter der Display-Entities aufrufbar. */
@Mixin(Display.class)
public interface DisplayAccessor {
    @Invoker("setTransformation")
    void begleiter$setTransformation(Transformation transformation);
}
