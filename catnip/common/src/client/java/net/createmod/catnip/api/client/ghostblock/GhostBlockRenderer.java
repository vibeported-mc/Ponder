package net.createmod.catnip.api.client.ghostblock;




import net.createmod.catnip.api.client.outliner.Outline;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.client.render.model.BakedModelBufferer;
import net.createmod.catnip.impl.client.placement.PlacementClient;
import net.createmod.catnip.impl.client.render.ColoringVertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class GhostBlockRenderer {
	private static final GhostBlockRenderer STANDARD = new DefaultGhostBlockRenderer();
	private static final GhostBlockRenderer TRANSPARENT = new TransparentGhostBlockRenderer();

	public static GhostBlockRenderer standard() {
		return STANDARD;
	}

	public static GhostBlockRenderer transparent() {
		return TRANSPARENT;
	}

	/**
	 * Minecraft 26.2 removed MultiBufferSource, so ghosts submit geometry to the collector instead of
	 * writing into a buffer source the caller flushes.
	 */
	public abstract void submit(PoseStack ms, SubmitNodeCollector queue, Vec3 camera, GhostBlockParams params);

	private static class DefaultGhostBlockRenderer extends GhostBlockRenderer {
		@Override
		public void submit(PoseStack ms, SubmitNodeCollector queue, Vec3 camera, GhostBlockParams params) {
			BlockState state = params.state;
			BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
			BlockPos pos = params.pos;

			ms.pushPose();
			ms.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
			BakedModelBufferer.submitModel(model, pos, state, ms, queue.order(Outline.ORDER_EARLY));
			ms.popPose();
		}
	}

	private static class TransparentGhostBlockRenderer extends GhostBlockRenderer {
		@Override
		public void submit(PoseStack ms, SubmitNodeCollector queue, Vec3 camera, GhostBlockParams params) {
			BlockState state = params.state;
			BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
			BlockPos pos = params.pos;
			float alpha = params.alphaSupplier.get() * .75f * PlacementClient.getCurrentAlpha();

			ms.pushPose();
			ms.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

			ms.translate(.5, .5, .5);
			ms.scale(.85f, .85f, .85f);
			ms.translate(-.5, -.5, -.5);

			// The alpha tint has to be applied per vertex, and submitBlockModel only offers per tint
			// index colors, so this buffers the model itself inside a custom geometry node.
			queue.order(Outline.ORDER_EARLY)
				.submitCustomGeometry(ms, Sheets.translucentBlockItemSheet(), (pose, consumer) -> {
					VertexConsumer vb = new ColoringVertexConsumer(consumer, 1, 1, 1, alpha);
					PoseStack local = new PoseStack();
					local.last().set(pose);
					BakedModelBufferer.bufferModel(model, pos, BlockAndTintGetter.EMPTY, state, local, (_, _) -> vb);
				});
			ms.popPose();
		}
	}
}
