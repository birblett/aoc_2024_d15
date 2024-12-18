package com.birblett.mixin;

import com.birblett.mixinterface.Solver;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStandEntityMixin extends LivingEntity implements Solver {

    @Unique int dy = 0;
    @Unique int dx = 0;
    @Unique Solver parent = null;
    @Unique int ticksAlive = 0;

    protected ArmorStandEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    public void setParameters(int dy, int dx, Solver parent) {
        this.dy = -dy;
        this.dx = dx;
        this.parent = parent;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickSolver(CallbackInfo ci) {
        ArmorStandEntity self = (ArmorStandEntity) (Object) this;
        if (self.getWorld() instanceof ServerWorld world && this.parent != null && this.ticksAlive++ == 20) {
            this.parent.incrementScore(this.dy, this.dx);
            world.breakBlock(self.getBlockPos().add(0, -1, 0), false);
        }
    }

    @Override
    public void kill() {
        if (this.parent != null) {
            ArmorStandEntity self = (ArmorStandEntity) (Object) this;
            if (self.getWorld() instanceof ServerWorld world) world.breakBlock(self.getBlockPos().add(0, -1, 0), false);
        }
        this.remove(Entity.RemovalReason.KILLED);
        this.emitGameEvent(GameEvent.ENTITY_DIE);
    }

}
