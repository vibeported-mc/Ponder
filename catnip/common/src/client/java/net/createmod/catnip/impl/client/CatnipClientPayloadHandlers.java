package net.createmod.catnip.impl.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.config.ConfigHelper;
import net.createmod.catnip.impl.network.CatnipPayloads;
import net.createmod.catnip.impl.network.ClientboundConfigPacket;
import net.createmod.catnip.impl.network.ClientboundSimpleActionPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;

public final class CatnipClientPayloadHandlers {
	private static final Logger logger = LogUtils.getLogger();

	public static void register() {
		ClientNetworkHelper.INSTANCE.registerPayloadHandler(CatnipPayloads.CLIENTBOUND_CONFIG, CatnipClientPayloadHandlers::config);
		ClientNetworkHelper.INSTANCE.registerPayloadHandler(CatnipPayloads.SIMPLE_ACTION, CatnipClientPayloadHandlers::action);

		ClientboundSimpleActionPacket.addAction("configScreen", () -> SimpleCatnipActions::configScreen);
	}

	private static void config(ClientboundConfigPacket payload, LocalPlayer player) {
		ConfigHelper.ConfigPath path;

		try {
			path = ConfigHelper.ConfigPath.parse(payload.path());
		} catch (IllegalArgumentException e) {
			player.sendSystemMessage(Component.literal(String.valueOf(e.getMessage())));
			return;
		}

		if (path.getType() != ModConfig.Type.CLIENT) {
			logger.warn("Received type-mismatched config packet on client");
			return;
		}

		try {
			ConfigHelper.setConfigValue(path, payload.value());
			player.sendSystemMessage(Component.literal("Great Success!"));
		} catch (ConfigHelper.InvalidValueException e) {
			player.sendSystemMessage(Component.literal("Config could not be set the the specified value!"));
		} catch (Exception e) {
			player.sendSystemMessage(Component.literal("Something went wrong while trying to set config value. Check the client logs for more information"));
			logger.warn("Exception during client-side config value set:", e);
		}
	}

	private static void action(ClientboundSimpleActionPacket payload, LocalPlayer player) {
		String name = payload.action();
		Supplier<Consumer<String>> action = ClientboundSimpleActionPacket.ACTIONS.get(name);

		if (action == null) {
			logger.warn("Received ClientboundSimpleActionPacket with invalid Action {}, ignoring the packet", name);
			return;
		}

		action.get().accept(payload.value());
	}
}
