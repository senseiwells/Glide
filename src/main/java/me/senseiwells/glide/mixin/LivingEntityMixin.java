package me.senseiwells.glide.mixin;

import me.senseiwells.glide.event.PlayerHorizontalFlyFallCollisionEvent;
import net.casual.arcade.events.GlobalEventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@ModifyVariable(
		method = "travel",
		at = @At(value = "STORE"),
		ordinal = 1,
		slice = @Slice(
			from = @At(
				value = "INVOKE",
				target = "Lnet/minecraft/world/phys/Vec3;horizontalDistance()D",
				ordinal = 1
			),
			to = @At(
				value = "INVOKE",
				target = "Lnet/minecraft/world/damagesource/DamageSources;flyIntoWall()Lnet/minecraft/world/damagesource/DamageSource;"
			)
		)
	)
	@SuppressWarnings("InvalidInjectorMethodSignature")
	private float onFlyFallHorizontalCollision(float original) {
		if ((Object) this instanceof ServerPlayer player) {
			PlayerHorizontalFlyFallCollisionEvent event = new PlayerHorizontalFlyFallCollisionEvent(player, original);
			GlobalEventHandler.broadcast(event);
			if (event.isCancelled()) {
				return -1.0F;
			}
		}
		return original;
	}
}
