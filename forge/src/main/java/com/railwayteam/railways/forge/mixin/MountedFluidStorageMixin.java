package com.railwayteam.railways.forge.mixin;

import com.railwayteam.railways.content.fuel.tank.FuelTankBlockEntity;
import com.simibubi.create.content.contraptions.MountedFluidStorage;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MountedFluidStorage.class, remap = false)
public abstract class MountedFluidStorageMixin {
    @Shadow
    protected abstract void onFluidStackChanged(FluidStack fs);

    @Shadow private BlockEntity blockEntity;

    @Shadow SmartFluidTank tank;

    @Inject(method = "canUseAsStorage", at = @At("HEAD"), cancellable = true)
    private static void canUseAsStorage(BlockEntity be, CallbackInfoReturnable<Boolean> cir) {
        if (be instanceof FuelTankBlockEntity fuelTankBlockEntity)
            cir.setReturnValue(fuelTankBlockEntity.isController());
    }

    @Inject(method = "createMountedTank", at = @At("HEAD"), cancellable = true)
    private void createMountedTank(BlockEntity be, CallbackInfoReturnable<SmartFluidTank> cir) {
        if (be instanceof FuelTankBlockEntity)
            cir.setReturnValue(new SmartFluidTank(
                    ((FuelTankBlockEntity) be).getTotalTankSize() * FuelTankBlockEntity.getCapacityMultiplier(),
                    this::onFluidStackChanged));
    }

    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "classValue=com/simibubi/create/content/fluids/tank/FluidTankBlockEntity",
            ordinal = 0)
    )
    public void tick(Entity entity, BlockPos pos, boolean isRemote, CallbackInfo ci) {
        if (blockEntity instanceof FuelTankBlockEntity be)
            be.getFluidLevel().tickChaser();
    }

    @Inject(method = "updateFluid", at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/fluid/SmartFluidTank;setFluid(Lnet/minecraftforge/fluids/FluidStack;)V",
            ordinal = 0,
            shift = At.Shift.AFTER
    ), cancellable = true)
    public void updateFluid(FluidStack fluid, CallbackInfo ci) {
        if (blockEntity instanceof FuelTankBlockEntity tankBE) {
            float fillState = tank.getFluidAmount() / (float) tank.getCapacity();
            if (tankBE.getFluidLevel() == null)
                tankBE.setFluidLevel(LerpedFloat.linear().startWithValue(fillState));
            tankBE.getFluidLevel().chase(fillState, 0.5, LerpedFloat.Chaser.EXP);
            IFluidTank tankInventory = tankBE.getTankInventory();
            if (tankInventory instanceof SmartFluidTank smartFT)
                smartFT.setFluid(fluid);

            ci.cancel();
        }
    }
}
