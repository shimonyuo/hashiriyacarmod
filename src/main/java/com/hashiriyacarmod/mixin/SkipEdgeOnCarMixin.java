package com.hashiriyacarmod.mixin;

import com.hashiriyacarmod.CarGroundNormalHolder;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class SkipEdgeOnCarMixin {

    @Inject(
            method = "maybeBackOffFromEdge",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipEdgeWhenObbInSweep(Vec3 movement, MoverType type, CallbackInfoReturnable<Vec3> cir) {
        Player self = (Player) (Object) this;

        // CarHitboxSupportMixin でセットされたフラグを使う
        if (self instanceof CarGroundNormalHolder holder) {
            if (holder.hashiriyacarmod$shouldSkipEdge()) {
                // エッジ後退をスキップして、元の移動量をそのまま返す
                cir.setReturnValue(movement);
            }
        }
    }
}