package org.dynmap.fabric_1_14_1.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.dynmap.fabric_1_14_1.DynmapFabric;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftDedicatedServer.class)
public class MinecraftServerMixin {
    @Inject(at = @At("TAIL"), method = "setupServer")
    private void onSetupServer(CallbackInfoReturnable<Boolean> info) {
        DynmapFabric.start();
    }

    @Inject(at = @At("TAIL"), method = "tickWorlds")
    private void onTickWorlds(BooleanSupplier supplier, CallbackInfo info) {
        DynmapFabric.tick();
    }

    @Inject(at = @At("HEAD"), method = "exit")
    private void onExit(CallbackInfo info) {
        DynmapFabric.stop();
    }
}
