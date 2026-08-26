package net.createmod.catnip.impl.neoforge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.createmod.catnip.api.client.event.LevelRenderCallback;
import net.createmod.catnip.api.client.event.LevelRendererReloadCallback;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	/**
	 * Minecraft 26.2 gathers everything it will draw before drawing any of it, and NeoForge's render
	 * stage events all fire during execution, which is too late to add nodes.
	 */
	@Inject(method = "submitFeatures", at = @At("TAIL"))
	private void catnip$onSubmitFeatures(LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, boolean renderOutline, CallbackInfo ci) {
		LevelRenderCallback.SUBMIT_FEATURES.invoker()
			.onSubmit(levelRenderState, submitNodeCollector, new PoseStack());
	}

	/**
	 * allChanged became resetLevelRenderData when chunk rebuilds moved to the level extractor.
	 */
	@Inject(method = "resetLevelRenderData", at = @At("TAIL"))
	private void catnip$onReload(CallbackInfo ci) {
		LevelRendererReloadCallback.EVENT.invoker().onReload();
	}
}
