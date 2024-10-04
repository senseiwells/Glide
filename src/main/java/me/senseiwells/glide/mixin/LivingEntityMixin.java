package me.senseiwells.glide.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.senseiwells.glide.event.PlayerHorizontalFlyFallCollisionEvent;
import me.senseiwells.glide.extension.PlayerGlideExtension;
import net.casual.arcade.events.GlobalEventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

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

	@ModifyExpressionValue(
		method = "travel",
		at = @At(
			value = "CONSTANT",
			args = "doubleValue=0.75"
		)
	)
	private double modifyThrustValue(double original) {
		if ((Object) this instanceof ServerPlayer player) {
			PlayerGlideExtension glide = PlayerGlideExtension.getGlide(player);
			return glide.getLiftForceModifier() * original;
		}
		return original;
	}

	// @Redirect(
	// 	method = "travel",
	// 	at = @At(
	// 		value = "INVOKE",
	// 		target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
	// 	),
	// 	slice = @Slice(
	// 		from = @At(
	// 			value = "INVOKE",
	// 			target = "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z"
	// 		)
	// 	)
	// )
	// private void onMove(LivingEntity instance, MoverType moverType, Vec3 vec3) {
	// 	if ((Object) this instanceof ServerPlayer player) {
	// 		PlayerGlideExtension glide = PlayerGlideExtension.getGlide(player);
	// 		double thermalDeltaMovement = glide.getThermalDeltaMovement();
	// 		if (thermalDeltaMovement != 0) {
	// 			Vec3 delta = player.getDeltaMovement();
	// 			Vec3 thermalDelta = new Vec3(delta.x, thermalDeltaMovement, delta.z);
	// 			player.move(moverType, thermalDelta);
	// 			player.setDeltaMovement(delta.x, 0.0, delta.z);
	// 			player.connection.send(
	// 				new ClientboundSetEntityMotionPacket(player.getId(), thermalDelta)
	// 			);
	// 			return;
	// 		}
	// 	}
	// 	instance.move(moverType, vec3);
	// }
}
