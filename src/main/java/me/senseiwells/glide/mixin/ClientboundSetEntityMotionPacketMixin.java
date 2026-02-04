package me.senseiwells.glide.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.senseiwells.glide.extension.PlayerGlideExtension;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientboundSetEntityMotionPacket.class)
public class ClientboundSetEntityMotionPacketMixin {
    @ModifyExpressionValue(
        method = "<init>(Lnet/minecraft/world/entity/Entity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private static Vec3 getGlideDeltaMovement(Vec3 original, Entity entity) {
        if (entity instanceof ServerPlayer player) {
            PlayerGlideExtension glide = PlayerGlideExtension.getGlide(player);
            double glideVerticalVelocity = glide.getGlideVerticalVelocity();
            if (glideVerticalVelocity != 0.0) {
                return new Vec3(original.x, glideVerticalVelocity * glide.getGlideNetworkScale(), original.z);
            }
        }
        return original;
    }
}
