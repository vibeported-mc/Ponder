package net.createmod.catnip.impl.client;

import net.createmod.catnip.api.client.config.ConfigModListScreen;
import net.createmod.catnip.api.client.config.SubMenuConfigScreen;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.createmod.catnip.api.config.ConfigHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * What the client does when the server asks for a {@code ClientboundSimpleActionPacket} action.
 *
 * <h2>26.2 note</h2>
 * <p>These open screens, so they live with the client code and are registered from
 * {@link CatnipClientPayloadHandlers#register()}; the packet itself is common and cannot name them.
 */
public class SimpleCatnipActions {

	public static void configScreen(String value) {
		if (value.isEmpty()) {
			ScreenOpener.open(new ConfigModListScreen(null));
			return;
		}

		LocalPlayer player = Minecraft.getInstance().player;

		if (player == null)
			return;

		ConfigHelper.ConfigPath configPath;
		try {
			configPath = ConfigHelper.ConfigPath.parse(value);
		} catch (IllegalArgumentException e) {
			player.sendSystemMessage(Component.literal(e.getMessage()));
			return;
		}

		try {
			ScreenOpener.open(SubMenuConfigScreen.find(configPath));
		} catch (Exception e) {
			player.sendSystemMessage(Component.literal("[Catnip]: ").withStyle(ChatFormatting.YELLOW).append(Component.translatable("catnip.util.unable_to_find_config.message").withStyle(ChatFormatting.WHITE)));
		}
	}

}
