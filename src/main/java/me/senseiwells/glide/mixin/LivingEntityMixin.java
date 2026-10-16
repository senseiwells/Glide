package me.senseiwells.glide.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.senseiwells.glide.event.PlayerHorizontalFlyFallCollisionEvent;
import me.senseiwells.glide.extension.PlayerGlideExtension;
import net.casual.arcade.events.GlobalEventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	public LivingEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Expression("(float) (? * 10.0 - 3.0)")
	@ModifyExpressionValue(
		method = "handleFallFlyingCollisions",
		at = @At("MIXINEXTRAS:EXPRESSION")
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

	@Inject(
		method = "travelFallFlying",
		at = @At("HEAD")
	)
	private void checkGlideCollisions(Vec3 input, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player) {
			PlayerGlideExtension glide = PlayerGlideExtension.getGlide(player);
			glide.checkForCollisions();
		}
	}

	@WrapOperation(
		method = "travelFallFlying",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
		)
	)
	private void onMove(LivingEntity instance, MoverType moverType, Vec3 vec3, Operation<Void> original) {
		if ((Object) this instanceof ServerPlayer player) {
			PlayerGlideExtension glide = PlayerGlideExtension.getGlide(player);
			original.call(instance, moverType, glide.modifyVelocityWithLift(vec3));
			glide.afterFallFlyingMove();
			return;
		}
		original.call(instance, moverType, vec3);
	}

	@Definition(id = "square", method = "Lnet/minecraft/util/Mth;square(D)D")
	@Definition(id = "cos", method = "Ljava/lang/Math;cos(D)D")
	@Expression("square(cos(?))")
	@ModifyExpressionValue(
		method = "updateFallFlyingMovement",
		at = @At("MIXINEXTRAS:EXPRESSION")
	)
	private double modifyVerticalBoost(double original) {
		if ((Object) this instanceof ServerPlayer player) {
			PlayerGlideExtension glide = PlayerGlideExtension.getGlide(player);
			return original * glide.getLiftForceModifier();
		}
		return original;
	}
}
