package net.createmod.catnip.impl.neoforge;

import net.createmod.catnip.api.Catnip;
import net.createmod.catnip.api.config.ConfigPathArgument;
import net.createmod.catnip.api.event.ServerCommandRegistrationCallback;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.BuiltInRegistries;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Catnip.ID)
@EventBusSubscriber
public final class CatnipNeoforge {
	// /catnip config sends this argument to clients in the command tree, which fails for a type
	// that is not registered
	private static final DeferredRegister<ArgumentTypeInfo<?, ?>> commandArgumentTypes = DeferredRegister.create(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, Catnip.ID);

	private static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<ConfigPathArgument>> CONFIG_PATH_ARGUMENT_TYPE = commandArgumentTypes.register(
		"config_path", () -> ArgumentTypeInfos.registerByClass(ConfigPathArgument.class, SingletonArgumentInfo.contextFree(ConfigPathArgument::new))
	);

	public CatnipNeoforge(IEventBus bus) {
		commandArgumentTypes.register(bus);
		bus.addListener(CatnipNeoforge::setup);
	}

	public static void setup(FMLCommonSetupEvent event) {
		event.enqueueWork(Catnip::init);
	}

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		ServerCommandRegistrationCallback.EVENT.invoker().register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
	}
}
