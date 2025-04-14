package me.senseiwells.glide.mixin;

import me.senseiwells.glide.event.PlayerHorizontalFlyFallCollisionEvent;
import me.senseiwells.glide.extension.PlayerGlideExtension;
import net.casual.arcade.events.GlobalEventHandler;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@ModifyVariable(
		method = "handleFallFlyingCollisions",
		at = @At("STORE")
	)
	private float onFlyFallHorizontalCollision(float original) {
		if ((Object) this instanceof ServerPlayer player) {
			PlayerHorizontalFlyFallCollisionEvent event = new PlayerHorizontalFlyFallCollisionEvent(player, original);
			GlobalEventHandler.Server.broadcast(event);
			if (event.isCancelled()) {
				return -1.0F;
			}
		}
		return original;
	}


	@Redirect(
		method = "travelFallFlying",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
		)
	)
	private void onMove(LivingEntity instance, MoverType moverType, Vec3 vec3) {
		if ((Object) this instanceof ServerPlayer player) {
			PlayerGlideExtension glide = PlayerGlideExtension.getGlide(player);
			double thermalDeltaMovement = glide.getThermalDeltaMovement();
			if (thermalDeltaMovement != 0.0) {
				Vec3 initial = glide.getInitialEntryVelocity();
				Vec3 delta = player.getDeltaMovement();
				if (delta.length() > initial.length()) {
					delta = new Vec3(
							Mth.clamp(delta.x, -initial.length(), initial.length()),
							Mth.clamp(delta.y, -initial.length(), initial.y),
							Mth.clamp(delta.z, -initial.length(), initial.length()));
				}
				Vec3 thermalDelta = new Vec3(delta.x, (thermalDeltaMovement * glide.getLiftForceModifier()), delta.z);
				player.move(moverType, thermalDelta);
				player.setDeltaMovement(delta.x, 0.0, delta.z);
				player.connection.send(
					new ClientboundSetEntityMotionPacket(player.getId(), thermalDelta)
				);
				return;
			}
		}
		instance.move(moverType, vec3);
	}
}
