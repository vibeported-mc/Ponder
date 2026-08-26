package net.createmod.ponder.testmod.neoforge;

import net.createmod.ponder.testmod.client.TestmodClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * The NeoForge entry point for the testmod. Upstream only ever shipped the Fabric side of it.
 */
@Mod(value = "ponder_testmod", dist = Dist.CLIENT)
public final class PonderTestmodNeoForge {
	public PonderTestmodNeoForge(IEventBus modEventBus, ModContainer modContainer) {
		TestmodClient.init();
	}
}
