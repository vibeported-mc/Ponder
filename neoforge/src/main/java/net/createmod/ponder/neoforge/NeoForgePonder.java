package net.createmod.ponder.neoforge;

import java.util.Map;

import net.createmod.catnip.api.config.ConfigBase;
import net.createmod.ponder.api.Ponder;
import net.createmod.ponder.impl.command.PonderCommands;
import net.createmod.ponder.impl.config.PonderConfig;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Ponder.MOD_ID)
public class NeoForgePonder {
	public NeoForgePonder(ModContainer container, IEventBus modEventBus) {
		registerConfigs(container);
		modEventBus.addListener((ModConfigEvent.Loading event) -> PonderConfig.onLoad(event.getConfig()));
		modEventBus.addListener((ModConfigEvent.Reloading event) -> PonderConfig.onReload(event.getConfig()));
	}

	private static void registerConfigs(ModContainer container) {
		for (Map.Entry<ModConfig.Type, ConfigBase> entry : PonderConfig.registerConfigs()) {
			container.registerConfig(entry.getKey(), entry.getValue().specification);
		}
	}

	@EventBusSubscriber
	public static class Events {
		@SubscribeEvent
		public static void registerCommands(RegisterCommandsEvent event) {
			PonderCommands.register(event.getDispatcher());
		}
	}
}
