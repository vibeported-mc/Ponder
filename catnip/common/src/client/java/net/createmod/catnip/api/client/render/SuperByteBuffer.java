package net.createmod.catnip.api.client.render;

import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;
import org.joml.Quaternionfc;
import org.joml.Quaternionf;
import org.joml.Matrix4fc;
import org.joml.Matrix3fc;
import net.minecraft.client.renderer.block.BlockAndTintGetter;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.LightCoordsUtil;

public interface SuperByteBuffer {
	static int maxLight(int packedLight1, int packedLight2) {
		int blockLight1 = LightCoordsUtil.block(packedLight1);
		int skyLight1 = LightCoordsUtil.sky(packedLight1);
		int blockLight2 = LightCoordsUtil.block(packedLight2);
		int skyLight2 = LightCoordsUtil.sky(packedLight2);
		return LightCoordsUtil.pack(Math.max(blockLight1, blockLight2), Math.max(skyLight1, skyLight2));
	}

	void renderInto(PoseStack ms, VertexConsumer consumer);

	/**
	 * Copy everything configured on this buffer into an immutable state and reset the buffer.
	 * <p>
	 * This is the entry point for the 26.2 extract/submit split: call it during extraction, keep the
	 * returned state on your render state, and submit it later. See {@link SuperByteBufferRenderState}.
	 */
	SuperByteBufferRenderState extractRenderState();

	/**
	 * Shorthand for extracting a state and immediately queueing it.
	 */
	default void submit(PoseStack ms, RenderType renderType, OrderedSubmitNodeCollector queue) {
		extractRenderState().submit(ms, renderType, queue);
	}

	boolean isEmpty();

	PoseStack getTransforms();

	<Self extends SuperByteBuffer> Self reset();

	<Self extends SuperByteBuffer> Self color(int color);

	<Self extends SuperByteBuffer> Self color(int r, int g, int b, int a);

	<Self extends SuperByteBuffer> Self disableDiffuse();

	<Self extends SuperByteBuffer> Self shiftUV(SpriteShiftEntry entry);

	<Self extends SuperByteBuffer> Self shiftUVScrolling(SpriteShiftEntry entry, float scrollU, float scrollV);

	<Self extends SuperByteBuffer> Self shiftUVtoSheet(SpriteShiftEntry entry, float uTarget, float vTarget, int sheetSize);

	<Self extends SuperByteBuffer> Self overlay(int overlay);

	<Self extends SuperByteBuffer> Self light(int packedLight);

	/**
	 * Indicate that this buffer should look up the light coordinates in the level.
	 */
	<Self extends SuperByteBuffer> Self useLevelLight(BlockAndTintGetter level);

	/**
	 * Indicate that this buffer should look up the light coordinates in the level.
	 * Light Positions will be transformed by the passed Matrix before the lookup.
	 */
	<Self extends SuperByteBuffer> Self useLevelLight(BlockAndTintGetter level, Matrix4f lightTransform);

	// --- transforms ------------------------------------------------------------------------------
	//
	// Flywheel's Transform/Affine interfaces used to supply these, but this module is loader
	// agnostic and Flywheel is only on the platform modules' classpath. They are mirrored here,
	// under the same names, over the PoseStack this buffer already exposes.

	@SuppressWarnings("unchecked")
	default <Self extends SuperByteBuffer> Self mulPose(Matrix4fc pose) {
		getTransforms().last()
			.pose()
			.mul(pose);
		return (Self) this;
	}

	@SuppressWarnings("unchecked")
	default <Self extends SuperByteBuffer> Self mulNormal(Matrix3fc normal) {
		getTransforms().last()
			.normal()
			.mul(normal);
		return (Self) this;
	}

	default <Self extends SuperByteBuffer> Self transform(PoseStack.Pose pose) {
		this.<Self>mulPose(pose.pose());
		return this.mulNormal(pose.normal());
	}

	default <Self extends SuperByteBuffer> Self transform(PoseStack stack) {
		return transform(stack.last());
	}

	@SuppressWarnings("unchecked")
	default <Self extends SuperByteBuffer> Self translate(float x, float y, float z) {
		getTransforms().translate(x, y, z);
		return (Self) this;
	}

	default <Self extends SuperByteBuffer> Self translate(double x, double y, double z) {
		return translate((float) x, (float) y, (float) z);
	}

	default <Self extends SuperByteBuffer> Self translate(float v) {
		return translate(v, v, v);
	}

	default <Self extends SuperByteBuffer> Self translateX(float x) {
		return translate(x, 0, 0);
	}

	default <Self extends SuperByteBuffer> Self translateY(float y) {
		return translate(0, y, 0);
	}

	default <Self extends SuperByteBuffer> Self translateZ(float z) {
		return translate(0, 0, z);
	}

	default <Self extends SuperByteBuffer> Self translate(Vec3i vec) {
		return translate(vec.getX(), vec.getY(), vec.getZ());
	}

	default <Self extends SuperByteBuffer> Self translate(Vec3 vec) {
		return translate(vec.x, vec.y, vec.z);
	}

	default <Self extends SuperByteBuffer> Self translate(Vector3fc vec) {
		return translate(vec.x(), vec.y(), vec.z());
	}

	default <Self extends SuperByteBuffer> Self translateBack(float x, float y, float z) {
		return translate(-x, -y, -z);
	}

	default <Self extends SuperByteBuffer> Self translateBack(double x, double y, double z) {
		return translate(-x, -y, -z);
	}

	default <Self extends SuperByteBuffer> Self translateBack(Vec3 vec) {
		return translate(-vec.x, -vec.y, -vec.z);
	}

	default <Self extends SuperByteBuffer> Self translateBack(Vec3i vec) {
		return translate(-vec.getX(), -vec.getY(), -vec.getZ());
	}

	/**
	 * Move to the middle of the block, so that a rotation applied here turns about its centre.
	 */
	default <Self extends SuperByteBuffer> Self center() {
		return translate(.5f, .5f, .5f);
	}

	default <Self extends SuperByteBuffer> Self uncenter() {
		return translate(-.5f, -.5f, -.5f);
	}

	@SuppressWarnings("unchecked")
	default <Self extends SuperByteBuffer> Self scale(float x, float y, float z) {
		getTransforms().scale(x, y, z);
		return (Self) this;
	}

	default <Self extends SuperByteBuffer> Self scale(float factor) {
		return scale(factor, factor, factor);
	}

	@SuppressWarnings("unchecked")
	default <Self extends SuperByteBuffer> Self rotate(Quaternionfc quaternion) {
		getTransforms().mulPose(new Quaternionf(quaternion));
		return (Self) this;
	}

	default <Self extends SuperByteBuffer> Self rotate(float radians, float axisX, float axisY, float axisZ) {
		if (radians == 0)
			return (Self) this;
		return rotate(new Quaternionf().rotateAxis(radians, axisX, axisY, axisZ));
	}

	default <Self extends SuperByteBuffer> Self rotate(float radians, Direction axis) {
		Vec3i vec = axis.getUnitVec3i();
		return rotate(radians, vec.getX(), vec.getY(), vec.getZ());
	}

	default <Self extends SuperByteBuffer> Self rotate(float radians, Direction.Axis axis) {
		return rotate(radians, axis == Direction.Axis.X ? 1 : 0, axis == Direction.Axis.Y ? 1 : 0,
			axis == Direction.Axis.Z ? 1 : 0);
	}

	default <Self extends SuperByteBuffer> Self rotateDegrees(float degrees, Direction axis) {
		return rotate(degrees * Mth.DEG_TO_RAD, axis);
	}

	default <Self extends SuperByteBuffer> Self rotateX(float radians) {
		return rotate(radians, 1, 0, 0);
	}

	default <Self extends SuperByteBuffer> Self rotateY(float radians) {
		return rotate(radians, 0, 1, 0);
	}

	default <Self extends SuperByteBuffer> Self rotateZ(float radians) {
		return rotate(radians, 0, 0, 1);
	}

	default <Self extends SuperByteBuffer> Self rotateXDegrees(float degrees) {
		return rotateX(degrees * Mth.DEG_TO_RAD);
	}

	default <Self extends SuperByteBuffer> Self rotateYDegrees(float degrees) {
		return rotateY(degrees * Mth.DEG_TO_RAD);
	}

	default <Self extends SuperByteBuffer> Self rotateZDegrees(float degrees) {
		return rotateZ(degrees * Mth.DEG_TO_RAD);
	}

	default <Self extends SuperByteBuffer> Self rotateCentered(Quaternionfc quaternion) {
		this.<Self>center();
		this.<Self>rotate(quaternion);
		return uncenter();
	}

	default <Self extends SuperByteBuffer> Self rotateCentered(float radians, Direction axis) {
		this.<Self>center();
		this.<Self>rotate(radians, axis);
		return uncenter();
	}

	default <Self extends SuperByteBuffer> Self rotateCentered(float radians, Direction.Axis axis) {
		this.<Self>center();
		this.<Self>rotate(radians, axis);
		return uncenter();
	}

	default <Self extends SuperByteBuffer> Self rotateCenteredDegrees(float degrees, Direction axis) {
		return rotateCentered(degrees * Mth.DEG_TO_RAD, axis);
	}

	/**
	 * Offset by a hair, deterministically per seed, so that coplanar geometry drawn by different
	 * callers does not z-fight.
	 */
	default <Self extends SuperByteBuffer> Self nudge(int seed) {
		long randomBits = seed * 31L * 493286711L;
		randomBits = randomBits * randomBits * 4392167121L + randomBits * 98761L;
		float xNudge = (((randomBits >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
		float yNudge = (((randomBits >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
		float zNudge = (((randomBits >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
		return translate(xNudge, yNudge, zNudge);
	}

	//

	default void delete() {
	}

	default <Self extends SuperByteBuffer> Self color(Color color) {
		return this.color(
			color.getRed(),
			color.getGreen(),
			color.getBlue(),
			color.getAlpha()
		);
	}

	default <Self extends SuperByteBuffer> Self shiftUVScrolling(SpriteShiftEntry entry, float scrollV) {
		return this.shiftUVScrolling(entry, 0, scrollV);
	}

	@FunctionalInterface
	interface SpriteShiftFunc {
		void shift(float u, float v, Output output);

		interface Output {
			void accept(float u, float v);
		}
	}

	class ShiftOutput implements SpriteShiftFunc.Output {
		public float u;
		public float v;

		@Override
		public void accept(float u, float v) {
			this.u = u;
			this.v = v;
		}
	}
}
