package net.createmod.catnip.api.client.event;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.event.CatnipEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;

/// Invoked at several points during level rendering.
@FunctionalInterface
public interface LevelRenderCallback {
	/// Invoked while the level renderer is collecting the nodes it will draw.
	///
	/// Minecraft 26.2 submits render nodes up front and executes them later, so anything that wants to
	/// draw in the level has to be submitted here rather than during one of the render stages.
	CatnipEvent<LevelRenderCallback> SUBMIT_FEATURES = CatnipEvent.create(callbacks -> (state, queue, transforms) -> {
		for (LevelRenderCallback callback : callbacks) {
			callback.onSubmit(state, queue, transforms);
		}
	});

	void onSubmit(LevelRenderState state, SubmitNodeCollector queue, PoseStack transforms);
}
