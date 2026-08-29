package net.createmod.catnip.impl.neoforge.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.createmod.catnip.api.client.gui.HudElements;
import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * Collects hud elements while the mods are loading and hands them to the game when it asks.
 * <p>
 * It only ever asks once, and it asks early - before the work enqueued from client setup gets a
 * turn - so anything registered after that point would quietly never be drawn. Registering late is
 * therefore an error rather than a no-op.
 */
public final class NeoforgeHudElements implements HudElements {
	private static final Logger LOGGER = LoggerFactory.getLogger("catnip");
	private static final List<Registration> registrations = new ArrayList<>();

	private static boolean handedOver = false;

	@Override
	public synchronized void register(Identifier id, Element element) {
		if (handedOver) {
			LOGGER.error("Hud element {} was registered after the game asked for its hud layers, and will "
				+ "not be drawn. Register it while the mod is being constructed.", id);
			return;
		}

		registrations.add(new Registration(id, element::render));
	}

	public static synchronized void registerEvent(RegisterGuiLayersEvent event) {
		handedOver = true;
		registrations.forEach(registration -> event.registerAboveAll(registration.id, registration.layer));
	}

	private record Registration(Identifier id, GuiLayer layer) {}
}
