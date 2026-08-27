package net.createmod.catnip.api.client.render;


import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;

@SuppressWarnings("unchecked")
public class ShadeSeparatingSuperByteBuffer implements SuperByteBuffer {
	private static final Long2IntMap WORLD_LIGHT_CACHE = new Long2IntOpenHashMap();

	private final TemplateMesh template;
	private final int[] shadeSwapVertices;

	// Vertex Position and Normals
	private final PoseStack transforms = new PoseStack();

	// Vertex Coloring
	private float r, g, b, a;
	private boolean disableDiffuse;

	// Vertex Texture Coords
	@Nullable
	private SpriteShiftFunc spriteShiftFunc;

	// Vertex Overlay
	private boolean hasCustomOverlay;
	private int overlay;

	// Vertex Light
	private boolean hasCustomLight;
	private int packedLight;
	private boolean useLevelLight;
	@Nullable
	private BlockAndTintGetter levelWithLight;
	@Nullable
	private Matrix4f lightTransform;
	private final boolean invertFakeDiffuseNormal;

	// Reused objects
	private final Matrix4f modelMat = new Matrix4f();
	private final Matrix3f normalMat = new Matrix3f();
	private final Vector4f pos = new Vector4f();
	private final ShiftOutput shiftOutput = new ShiftOutput();
	private final Vector4f lightPos = new Vector4f();

	public ShadeSeparatingSuperByteBuffer(TemplateMesh template, int[] shadeSwapVertices, boolean invertFakeDiffuseNormal) {
		this.template = template;
		this.shadeSwapVertices = shadeSwapVertices;
		this.invertFakeDiffuseNormal = invertFakeDiffuseNormal;
		reset();
	}

	public ShadeSeparatingSuperByteBuffer(TemplateMesh template, int[] shadeSwapVertices) {
		this(template, shadeSwapVertices, false);
	}

	public ShadeSeparatingSuperByteBuffer(TemplateMesh template) {
		this(template, new int[0]);
	}

	public void renderInto(PoseStack input, VertexConsumer builder) {
		if (isEmpty()) {
			return;
		}
		snapshot().render(input.last(), builder);
		reset();
	}

	@Override
	public SuperByteBufferRenderState extractRenderState() {
		if (isEmpty()) {
			return SuperByteBufferRenderState.EMPTY;
		}
		Snapshot state = snapshot();
		// The buffer is pooled and will usually be handed to another renderer before the queue draws
		// this state, so it has to be released here rather than at draw time.
		reset();
		return state;
	}

	/**
	 * Copy the currently configured settings. The template and shade swap indices are immutable, so
	 * the snapshot can share them; everything else is mutable buffer state and has to be copied.
	 */
	private Snapshot snapshot() {
		return new Snapshot(template, shadeSwapVertices, new Matrix4f(transforms.last()
			.pose()), r, g, b, a, spriteShiftFunc, hasCustomLight, packedLight, useLevelLight, levelWithLight,
			lightTransform == null ? null : new Matrix4f(lightTransform));
	}

	private static final class Snapshot implements SuperByteBufferRenderState {
		private final TemplateMesh template;
		private final int[] shadeSwapVertices;
		private final Matrix4f localTransforms;
		private final float r, g, b, a;
		private final boolean tinted;
		private final @Nullable SpriteShiftFunc spriteShiftFunc;
		private final boolean hasCustomLight;
		private final int packedLight;
		private final boolean useLevelLight;
		private final @Nullable BlockAndTintGetter levelWithLight;
		private final @Nullable Matrix4f lightTransform;

		private Snapshot(TemplateMesh template, int[] shadeSwapVertices, Matrix4f localTransforms, float r, float g,
			float b, float a, @Nullable SpriteShiftFunc spriteShiftFunc, boolean hasCustomLight, int packedLight,
			boolean useLevelLight, @Nullable BlockAndTintGetter levelWithLight, @Nullable Matrix4f lightTransform) {
			this.template = template;
			this.shadeSwapVertices = shadeSwapVertices;
			this.localTransforms = localTransforms;
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
			this.tinted = r != 1 || g != 1 || b != 1 || a != 1;
			this.spriteShiftFunc = spriteShiftFunc;
			this.hasCustomLight = hasCustomLight;
			this.packedLight = packedLight;
			this.useLevelLight = useLevelLight;
			this.levelWithLight = levelWithLight;
			this.lightTransform = lightTransform;
		}

		/**
		 * Multiply a packed ARGB colour by the tint configured on the buffer. 26.2's block vertex
		 * format no longer carries normals or an overlay coordinate, so diffuse shading and the
		 * overlay are gone, but the per-vertex colour is still there to tint.
		 */
		private int tint(int color) {
			return ARGB.color((int) (ARGB.alpha(color) * a), (int) (ARGB.red(color) * r),
				(int) (ARGB.green(color) * g), (int) (ARGB.blue(color) * b));
		}

		@Override
		public void submit(PoseStack ms, RenderType renderType, OrderedSubmitNodeCollector queue) {
			queue.submitCustomGeometry(ms, renderType, this);
		}

		@Override
		public boolean isEmpty() {
			return template.isEmpty();
		}

		@Override
		public void render(PoseStack.Pose input, VertexConsumer builder) {
			if (useLevelLight) {
				WORLD_LIGHT_CACHE.clear();
			}

			// A submitted state can be drawn from a different thread than the one that extracted it,
			// so none of the buffer's scratch objects may be reused here.
			Matrix4f modelMat = new Matrix4f(input.pose());
			modelMat.mul(localTransforms);

			Vector4f pos = new Vector4f();
			ShiftOutput shiftOutput = new ShiftOutput();
			Vector4f lightPos = new Vector4f();

			boolean shaded = true;
			int shadeSwapIndex = 0;
			int nextShadeSwapVertex = shadeSwapIndex < shadeSwapVertices.length ? shadeSwapVertices[shadeSwapIndex] : -1;

			int vertexCount = template.vertexCount();
			for (int i = 0; i < vertexCount; i++) {
				if (i == nextShadeSwapVertex) {
					shaded = !shaded;
					shadeSwapIndex++;
					nextShadeSwapVertex = shadeSwapIndex < shadeSwapVertices.length ? shadeSwapVertices[shadeSwapIndex] : -1;
				}

				float x = template.x(i);
				float y = template.y(i);
				float z = template.z(i);
				pos.set(x, y, z, 1.0f);
				pos.mul(modelMat);

				int color = template.color(i);
				if (tinted) {
					color = tint(color);
				}

				float u = template.u(i);
				float v = template.v(i);
				if (spriteShiftFunc != null) {
					spriteShiftFunc.shift(u, v, shiftOutput);
					u = shiftOutput.u;
					v = shiftOutput.v;
				}

				int light = template.light(i);
				if (hasCustomLight) {
					light = SuperByteBuffer.maxLight(light, packedLight);
				}
				if (useLevelLight) {
					lightPos.set(((x - .5f) * 15 / 16f) + .5f, (y - .5f) * 15 / 16f + .5f, (z - .5f) * 15 / 16f + .5f, 1f);
					lightPos.mul(localTransforms);
					if (lightTransform != null) {
						lightPos.mul(lightTransform);
					}
					light = SuperByteBuffer.maxLight(light, getLight(levelWithLight, lightPos));
				}

				builder.addVertex(pos.x(), pos.y(), pos.z()).setColor(color).setUv(u, v).setLight(light);
			}
		}
	}

	public SuperByteBuffer reset() {
		while (!transforms.isEmpty())
			transforms.popPose();
		transforms.pushPose();

		r = 1;
		g = 1;
		b = 1;
		a = 1;
		disableDiffuse = false;
		spriteShiftFunc = null;
		hasCustomOverlay = false;
		overlay = OverlayTexture.NO_OVERLAY;
		hasCustomLight = false;
		packedLight = 0;
		useLevelLight = false;
		levelWithLight = null;
		lightTransform = null;
		return this;
	}

	public boolean isEmpty() {
		return template.isEmpty();
	}

	public PoseStack getTransforms() {
		return transforms;
	}

	public SuperByteBuffer color(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
		return this;
	}

	public SuperByteBuffer color(int r, int g, int b, int a) {
		color(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
		return this;
	}

	public SuperByteBuffer color(int color) {
		color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255);
		return this;
	}

	public SuperByteBuffer color(Color c) {
		return color(c.getRGB());
	}

	public SuperByteBuffer disableDiffuse() {
		disableDiffuse = true;
		return this;
	}

	public SuperByteBuffer shiftUV(SpriteShiftEntry entry) {
		spriteShiftFunc = (u, v, output) -> {
			output.accept(entry.getTargetU(u), entry.getTargetV(v));
		};
		return this;
	}

	public SuperByteBuffer shiftUVScrolling(SpriteShiftEntry entry, float scrollV) {
		return shiftUVScrolling(entry, 0, scrollV);
	}

	public SuperByteBuffer shiftUVScrolling(SpriteShiftEntry entry, float scrollU, float scrollV) {
		spriteShiftFunc = (u, v, output) -> {
			float targetU = u - entry.getOriginal()
				.getU0() + entry.getTarget()
				.getU0()
				+ scrollU;
			float targetV = v - entry.getOriginal()
				.getV0() + entry.getTarget()
				.getV0()
				+ scrollV;
			output.accept(targetU, targetV);
		};
		return this;
	}

	public SuperByteBuffer shiftUVtoSheet(SpriteShiftEntry entry, float uTarget, float vTarget, int sheetSize) {
		spriteShiftFunc = (u, v, output) -> {
			float targetU = entry.getTarget()
				.getU((SpriteShiftEntry.getUnInterpolatedU(entry.getOriginal(), u) / sheetSize) + uTarget);
			float targetV = entry.getTarget()
				.getV((SpriteShiftEntry.getUnInterpolatedV(entry.getOriginal(), v) / sheetSize) + vTarget);
			output.accept(targetU, targetV);
		};
		return this;
	}

	public SuperByteBuffer overlay(int overlay) {
		hasCustomOverlay = true;
		this.overlay = overlay;
		return this;
	}

	public SuperByteBuffer light(int packedLight) {
		hasCustomLight = true;
		this.packedLight = packedLight;
		return this;
	}

	@Override
	public SuperByteBuffer useLevelLight(BlockAndTintGetter level) {
		useLevelLight = true;
		levelWithLight = level;
		return this;
	}

	@Override
	public SuperByteBuffer useLevelLight(BlockAndTintGetter level, Matrix4f lightTransform) {
		useLevelLight = true;
		levelWithLight = level;
		this.lightTransform = lightTransform;
		return this;
	}

	// Adapted from minecraft:shaders/include/light.glsl
	private static float calculateDiffuse(Vector3fc normal, Vector3fc lightDir0, Vector3fc lightDir1) {
		float light0 = Math.max(0.0f, lightDir0.dot(normal));
		float light1 = Math.max(0.0f, lightDir1.dot(normal));
		return Math.min(1.0f, (light0 + light1) * 0.6f + 0.4f);
	}

	private static int getLight(BlockAndTintGetter world, Vector4f lightPos) {
		BlockPos pos = BlockPos.containing(lightPos.x(), lightPos.y(), lightPos.z());
		return WORLD_LIGHT_CACHE.computeIfAbsent(pos.asLong(), $ -> LightCoordsUtil.getLightCoords(world, pos));
	}
}
