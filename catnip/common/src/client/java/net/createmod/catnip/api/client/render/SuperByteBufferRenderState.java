package net.createmod.catnip.api.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * An immutable snapshot of a {@link SuperByteBuffer}, ready to be handed to the render queue.
 * <p>
 * Minecraft 26.2 splits block entity and entity rendering into an extract phase and a submit phase.
 * A {@code SuperByteBuffer} is mutable and pooled, so it cannot survive the gap between the two: by
 * the time the queue draws it, the buffer has usually been reset and reused by another renderer.
 * Extracting a state copies the settings that were configured on the buffer, leaving the buffer free
 * to be recycled immediately.
 */
public interface SuperByteBufferRenderState extends SubmitNodeCollector.CustomGeometryRenderer {
	/**
	 * Queue this geometry for drawing with the given render type.
	 */
	void submit(PoseStack ms, RenderType renderType, OrderedSubmitNodeCollector queue);

	/**
	 * Write this geometry straight into a consumer, bypassing the queue. Only valid where the caller
	 * already owns a live buffer, such as inside another {@code CustomGeometryRenderer}.
	 */
	@Override
	void render(PoseStack.Pose pose, VertexConsumer consumer);

	boolean isEmpty();

	SuperByteBufferRenderState EMPTY = new SuperByteBufferRenderState() {
		@Override
		public void submit(PoseStack ms, RenderType renderType, OrderedSubmitNodeCollector queue) {
		}

		@Override
		public void render(PoseStack.Pose pose, VertexConsumer consumer) {
		}

		@Override
		public boolean isEmpty() {
			return true;
		}
	};
}
