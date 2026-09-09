package net.createmod.catnip.api.client.gui.element;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.client.level.SinglePosVirtualBlockGetter;
import net.createmod.catnip.api.client.render.model.BakedModelBufferer;
import net.createmod.catnip.impl.client.render.ColoringVertexConsumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws a block model straight into a picture-in-picture pass that is already under way.
 *
 * <h2>Why a widget wants this</h2>
 * <p>{@link GuiGameElement} submits one picture-in-picture state per element, and every state is
 * drawn into a texture of its own, with a depth buffer of its own. Those textures are then composited
 * as flat quads in the order they were submitted, so two elements of the same machine cannot resolve
 * against each other in depth -- whichever went last simply covers the other. On 1.21.1 the parts
 * shared one framebuffer and one depth buffer, which is how the millstone's cog shows through the
 * hole in its top: the cog is nearer than the floor of the hole and wins on depth. Composited
 * separately it is hidden by the body no matter which way round they are drawn, and reversing the
 * order only trades one wrong occlusion for another.
 *
 * <p>So a widget made of several parts should open <b>one</b> pass and draw all of its parts into it,
 * which is what this is for: it takes the pose stack and collector a pass hands out and buffers a
 * model into them, leaving depth to sort the parts out the way it used to.
 */
public final class GuiElementGeometry {

	private GuiElementGeometry() {}

	public static void submitBlockModel(PoseStack poseStack, SubmitNodeCollector collector,
		BlockStateModel model, @Nullable BlockState blockState, @Nullable BlockEntity blockEntity, int color) {

		BlockState state = blockState == null ? Blocks.AIR.defaultBlockState() : blockState;

		SinglePosVirtualBlockGetter level = SinglePosVirtualBlockGetter.createFullBright();
		level.blockState(state);
		level.blockEntity(blockEntity);

		// A tint has to be applied per vertex, so the model is buffered inside a custom geometry node
		// rather than submitted as a block model.
		for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
			RenderType type = layer == ChunkSectionLayer.TRANSLUCENT
				? Sheets.translucentBlockItemSheet()
				: Sheets.cutoutBlockItemSheet();

			collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
				ColoringVertexConsumer tinted = new ColoringVertexConsumer(consumer,
					ARGB.red(color) / 255f, ARGB.green(color) / 255f, ARGB.blue(color) / 255f, 1);

				PoseStack local = new PoseStack();
				local.last().set(pose);
				BakedModelBufferer.bufferModel(model, BlockPos.ZERO, level, state, local,
					(bufferedLayer, shade) -> bufferedLayer == layer ? tinted : null);
			});
		}
	}

	/**
	 * Turns a part about the middle of its block, the way {@code GuiGameElement.rotateBlock} does.
	 */
	public static void rotateBlock(PoseStack poseStack, double xRot, double yRot, double zRot) {
		poseStack.translate(0.5F, 0.5F, 0.5F);
		poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) zRot));
		poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) xRot));
		poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) yRot));
		poseStack.translate(-0.5F, -0.5F, -0.5F);
	}
}
