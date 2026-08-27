package net.createmod.ponder.impl.client;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.platform.ModClientHooksHelper;
import net.createmod.catnip.api.client.render.SuperByteBufferCache;
import net.createmod.catnip.api.platform.services.PlatformHelper;
import net.createmod.catnip.impl.network.ClientboundSimpleActionPacket;
import net.createmod.ponder.api.client.PonderIndex;
import net.createmod.ponder.api.client.level.PonderLevel;
import net.createmod.ponder.impl.client.gui.PonderUI;
import net.createmod.ponder.impl.client.element.WorldSectionElementImpl;
import net.createmod.ponder.impl.client.gui.PonderSceneRenderState;
import net.createmod.ponder.impl.client.gui.PonderSceneRenderer;
import net.createmod.ponder.impl.client.plugin.BasePonderPlugin;
import net.createmod.ponder.impl.client.plugin.DebugPonderPlugin;
import net.createmod.ponder.impl.client.tooltip.PonderTooltipHandler;
import net.minecraft.world.level.LevelAccessor;

public class PonderClient {
	public static void init() {
		SuperByteBufferCache.getInstance().registerCompartment(WorldSectionElementImpl.PONDER_WORLD_SECTION);

		ClientboundSimpleActionPacket.addAction("openPonder", () -> SimplePonderActions::openPonder);
		ClientboundSimpleActionPacket.addAction("reloadPonder", () -> SimplePonderActions::reloadPonder);

		ModClientHooksHelper.INSTANCE.registerPictureInPictureRenderer(PonderSceneRenderState.class, PonderSceneRenderer::new);

		PonderTooltipHandler.init();

		// A ponder scene runs on its own clock, so animations inside one follow the scene rather than
		// the world the player left behind.
		AnimationTickHolder.registerAlternateClock(new AnimationTickHolder.AlternateClock() {
			@Override
			public boolean appliesTo(LevelAccessor level) {
				return level instanceof PonderLevel;
			}

			@Override
			public int ticks() {
				return PonderUI.ponderTicks;
			}

			@Override
			public float partialTicks() {
				return PonderUI.getPartialTicks();
			}
		});

		PonderIndex.addPlugin(new BasePonderPlugin());

		if (PlatformHelper.INSTANCE.isDevelopmentEnvironment()) {
			PonderIndex.addPlugin(new DebugPonderPlugin());
		}
	}

	public static void modLoadCompleted() {
		PonderIndex.registerAll();
	}
}
