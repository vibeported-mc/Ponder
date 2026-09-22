package net.createmod.ponder.neoforge;

import net.createmod.catnip.api.client.config.BaseConfigScreen;
import net.createmod.ponder.api.Ponder;
import net.createmod.ponder.api.client.event.TooltipQueryCallback;
import net.createmod.ponder.impl.client.PonderClient;
import net.createmod.ponder.impl.client.PonderKeybinds;
import net.createmod.ponder.impl.config.PonderConfig;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@Mod(value = Ponder.MOD_ID, dist = Dist.CLIENT)
public class NeoForgePonderClient {
	public NeoForgePonderClient(ModContainer container, IEventBus modEventBus) {
		PonderClient.init();
		modEventBus.addListener(NeoForgePonderClient::init);

		container.registerExtensionPoint(IConfigScreenFactory.class,
			(mc, previousScreen) -> new BaseConfigScreen(previousScreen, Ponder.MOD_ID));
		BaseConfigScreen.setDefaultActionFor(Ponder.MOD_ID, base -> base
			.withButtonLabels("Client Settings", null, null)
			.withSpecs(PonderConfig.client().specification, null, null)
		);
	}

	public static void init(FMLClientSetupEvent event) {
	}

	@EventBusSubscriber(Dist.CLIENT)
	public static class ClientEvents {
		@SubscribeEvent
		public static void onItemTooltip(ItemTooltipEvent event) {
			TooltipQueryCallback.EVENT.invoker().onTooltipQuery(event.getItemStack(), event.getContext(), event.getFlags(), event.getToolTip());
		}
	}

	@EventBusSubscriber(value = Dist.CLIENT)
	public static class ModBusClientEvents {
		@SubscribeEvent
		public static void loadCompleted(FMLLoadCompleteEvent event) {
			PonderClient.modLoadCompleted();
		}

		@SubscribeEvent
		public static void register(RegisterKeyMappingsEvent event) {
			PonderKeybinds.register(event::register);
		}
	}

}
