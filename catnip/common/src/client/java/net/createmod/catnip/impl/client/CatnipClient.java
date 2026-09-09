package net.createmod.catnip.impl.client;


import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.Catnip;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.event.AtlasStitchedCallback;
import net.createmod.catnip.api.client.event.ClientTickCallback;
import net.createmod.catnip.api.client.event.LevelRenderCallback;
import net.createmod.catnip.api.client.event.LevelRendererReloadCallback;
import net.createmod.catnip.api.client.ghostblock.GhostBlocks;
import net.createmod.catnip.api.client.gui.HudElements;
import net.createmod.catnip.api.client.gui.render.pip.GuiBlockEntityRenderState;
import net.createmod.catnip.api.client.gui.render.pip.GuiBlockModelRenderState;
import net.createmod.catnip.api.client.gui.render.pip.GuiFluidStateRenderState;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.createmod.catnip.api.client.platform.ModClientHooksHelper;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.CatnipRenderPipelines;
import net.createmod.catnip.api.client.render.StitchedSprite;
import net.createmod.catnip.api.client.render.SuperByteBufferCache;
import net.createmod.catnip.api.data.ReloadListenerRegistries;
import net.createmod.catnip.impl.client.gui.element.pip.GuiBlockEntityRenderer;
import net.createmod.catnip.impl.client.gui.element.pip.GuiBlockModelRenderer;
import net.createmod.catnip.impl.client.gui.element.pip.GuiFluidStateRenderer;
import net.createmod.catnip.impl.client.placement.PlacementClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;

public final class CatnipClient {
	public static void preInit() {
		CatnipRenderPipelines.init();

		// Has to happen here rather than in init: the game asks for its hud layers before the work
		// enqueued from client setup gets a turn, so anything registered there arrives too late to
		// be drawn.
		HudElements.INSTANCE.register(Catnip.id("placement_helper"), PlacementClient::renderCrosshairOverlay);

		// And these for the same reason. The picture-in-picture renderers are gathered when the game
		// builds its GUI renderer, which happens before the work enqueued from client setup runs --
		// so registering them in init put them in a map that had already been read. Nothing said so:
		// a state whose class has no registered renderer is simply never drawn, which emptied every
		// block out of every Create screen, JEI recipe panels included.
		ModClientHooksHelper.INSTANCE.registerPictureInPictureRenderer(GuiBlockModelRenderState.class, GuiBlockModelRenderer::new);
		ModClientHooksHelper.INSTANCE.registerPictureInPictureRenderer(GuiBlockEntityRenderState.class, GuiBlockEntityRenderer::new);
		ModClientHooksHelper.INSTANCE.registerPictureInPictureRenderer(GuiFluidStateRenderState.class, GuiFluidStateRenderer::new);
	}

	public static void init() {
		SuperByteBufferCache.getInstance().registerCompartment(CachedBuffers.GENERIC_BLOCK);
	    CatnipClientPayloadHandlers.register();

		ClientTickCallback.EVENT.pre().subscribe(CatnipClient::beforeClientTick);
		LevelRendererReloadCallback.EVENT.subscribe(CatnipClient::onRendererReload);
		LevelRenderCallback.SUBMIT_FEATURES.subscribe(CatnipClient::onSubmitFeatures);
		AtlasStitchedCallback.EVENT.subscribe(StitchedSprite::afterAtlasStitch);

		ReloadListenerRegistries.INSTANCE.assets().register(CatnipReloadListener.ID, CatnipReloadListener.INSTANCE);
	}

	private static void beforeClientTick() {
		AnimationTickHolder.tick();

		if (!isGameActive())
			return;

		PlacementClient.tick(); // Should be called before GhostBlocks' tick as it can add new ghosts

		GhostBlocks.getInstance().tickGhosts();
		Outliner.getInstance().tickOutlines();
	}

	private static void onRendererReload() {
		AnimationTickHolder.reset();
		SuperByteBufferCache.getInstance().invalidate();
	}

	/**
	 * Minecraft 26.2 collects render nodes before drawing them, so outlines and ghosts have to be
	 * submitted during the submit phase rather than drawn during a render stage.
	 */
	public static void onSubmitFeatures(LevelRenderState state, SubmitNodeCollector queue, PoseStack transforms) {
		Vec3 cameraPos = state.cameraRenderState.pos;
		float partialTicks = AnimationTickHolder.getPartialTicks();

		transforms.pushPose();

		GhostBlocks.getInstance().submitAll(transforms, queue, cameraPos);
		Outliner.getInstance().submitOutlines(transforms, queue, cameraPos, partialTicks);

		transforms.popPose();
	}

	public static boolean isGameActive() {
		return Minecraft.getInstance().level != null && Minecraft.getInstance().player != null;
	}
}
