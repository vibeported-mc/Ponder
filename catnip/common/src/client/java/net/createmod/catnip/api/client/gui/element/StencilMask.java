package net.createmod.catnip.api.client.gui.element;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.ARGB;

/**
 * Masks one drawing with another, the way a {@link StencilElement} asks for.
 *
 * <h2>26.2 note</h2>
 * <p>1.21.1 wrote the mask into the stencil buffer and drew the element through it, with GL calls
 * made while rendering. 26.2 only collects render states while a screen is extracted and draws them
 * later, batched and reordered, so GL state set during extraction masks nothing -- the element was
 * painted straight over its mask.
 *
 * <p>Instead, both drawings are extracted into scratch {@link GuiRenderState}s. The element's
 * coloured geometry becomes a {@link ColorField}, and each of the mask's render states is re-emitted
 * into the real frame with every vertex recoloured from that field. Catnip's elements are gradients
 * and flat fills, which interpolate linearly across a quad, so sampling at the vertices reproduces
 * them exactly; a textured mask such as an icon keeps its texture, which the GUI pipelines multiply
 * by the vertex colour.
 */
public final class StencilMask {

	private StencilMask() {}

	/**
	 * Draws {@code mask}, coloured by {@code element}, at the current pose. Both are drawn in the
	 * same local space, as they would have been into the stencil buffer.
	 */
	public static void draw(GuiGraphicsExtractor graphics, Consumer<GuiGraphicsExtractor> mask, Consumer<GuiGraphicsExtractor> element) {
		ColorField field = ColorField.of(capture(element));
		if (field.isEmpty())
			return;

		Matrix3x2f pose = new Matrix3x2f(graphics.pose());
		@Nullable ScreenRectangle scissor = UIRenderHelper.getScissor(graphics);
		capture(mask).forEachElement(
			state -> graphics.guiRenderState.addGuiElement(new RecoloredRenderState(state, pose, field, scissor)),
			GuiRenderState.TraverseRange.ALL
		);
	}

	/**
	 * Extracts a drawing into a scratch state at the identity pose, so its vertices come out in the
	 * drawing's own local space.
	 */
	public static GuiRenderState capture(Consumer<GuiGraphicsExtractor> drawing) {
		GuiRenderState state = new GuiRenderState();
		drawing.accept(new GuiGraphicsExtractor(Minecraft.getInstance(), state, 0, 0));
		return state;
	}

	/**
	 * The colour a set of GUI render states paints at each point, composited in draw order.
	 */
	public static final class ColorField {
		// x0, y0, x1, y1, x2, y2 per triangle, with the matching corner colours in colors
		private final float[] positions;
		private final int[] colors;

		private ColorField(float[] positions, int[] colors) {
			this.positions = positions;
			this.colors = colors;
		}

		public static ColorField of(GuiRenderState state) {
			List<Vertex> vertices = new ArrayList<>();
			List<Integer> triangleColors = new ArrayList<>();
			List<Float> trianglePositions = new ArrayList<>();

			state.forEachElement(element -> {
				vertices.clear();
				element.buildVertices(new Recorder(vertices));
				triangulate(element.pipeline().getPrimitiveTopology(), vertices, (a, b, c) -> {
					for (Vertex v : new Vertex[] {a, b, c}) {
						trianglePositions.add(v.x);
						trianglePositions.add(v.y);
						triangleColors.add(v.color);
					}
				});
			}, GuiRenderState.TraverseRange.ALL);

			float[] positions = new float[trianglePositions.size()];
			for (int i = 0; i < positions.length; i++)
				positions[i] = trianglePositions.get(i);
			int[] colors = new int[triangleColors.size()];
			for (int i = 0; i < colors.length; i++)
				colors[i] = triangleColors.get(i);
			return new ColorField(positions, colors);
		}

		public boolean isEmpty() {
			return colors.length == 0;
		}

		/**
		 * @return the ARGB colour at a point, or fully transparent where nothing was drawn
		 */
		public int sample(float x, float y) {
			// composited "source over", in premultiplied form
			float r = 0, g = 0, b = 0, a = 0;

			for (int t = 0; t < colors.length; t += 3) {
				int p = t * 2;
				float x0 = positions[p], y0 = positions[p + 1];
				float x1 = positions[p + 2], y1 = positions[p + 3];
				float x2 = positions[p + 4], y2 = positions[p + 5];

				float det = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2);
				if (det == 0)
					continue;

				float w0 = ((y1 - y2) * (x - x2) + (x2 - x1) * (y - y2)) / det;
				float w1 = ((y2 - y0) * (x - x2) + (x0 - x2) * (y - y2)) / det;
				float w2 = 1 - w0 - w1;
				float epsilon = -1e-4f;
				if (w0 < epsilon || w1 < epsilon || w2 < epsilon)
					continue;

				int c0 = colors[t], c1 = colors[t + 1], c2 = colors[t + 2];
				float sa = (ARGB.alpha(c0) * w0 + ARGB.alpha(c1) * w1 + ARGB.alpha(c2) * w2) / 255f;
				float sr = (ARGB.red(c0) * w0 + ARGB.red(c1) * w1 + ARGB.red(c2) * w2) / 255f;
				float sg = (ARGB.green(c0) * w0 + ARGB.green(c1) * w1 + ARGB.green(c2) * w2) / 255f;
				float sb = (ARGB.blue(c0) * w0 + ARGB.blue(c1) * w1 + ARGB.blue(c2) * w2) / 255f;

				r = sr * sa + r * (1 - sa);
				g = sg * sa + g * (1 - sa);
				b = sb * sa + b * (1 - sa);
				a = sa + a * (1 - sa);
			}

			if (a <= 0)
				return 0;

			return ARGB.colorFromFloat(Math.min(a, 1), Math.min(r / a, 1), Math.min(g / a, 1), Math.min(b / a, 1));
		}
	}

	@FunctionalInterface
	private interface TriangleSink {
		void accept(Vertex a, Vertex b, Vertex c);
	}

	private static final class Vertex {
		final float x, y;
		// a vertex that never sets a colour is drawn white
		int color = -1;

		Vertex(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}

	private static void triangulate(PrimitiveTopology topology, List<Vertex> v, TriangleSink sink) {
		switch (topology) {
			case QUADS -> {
				for (int i = 0; i + 3 < v.size(); i += 4) {
					sink.accept(v.get(i), v.get(i + 1), v.get(i + 2));
					sink.accept(v.get(i), v.get(i + 2), v.get(i + 3));
				}
			}
			case TRIANGLES -> {
				for (int i = 0; i + 2 < v.size(); i += 3)
					sink.accept(v.get(i), v.get(i + 1), v.get(i + 2));
			}
			case TRIANGLE_STRIP -> {
				for (int i = 0; i + 2 < v.size(); i++)
					sink.accept(v.get(i), v.get(i + 1), v.get(i + 2));
			}
			case TRIANGLE_FAN -> {
				for (int i = 1; i + 1 < v.size(); i++)
					sink.accept(v.getFirst(), v.get(i), v.get(i + 1));
			}
			default -> {
				// lines and points cover no area to sample
			}
		}
	}

	/**
	 * Keeps each vertex's position and colour; everything else is ignored.
	 */
	private static final class Recorder implements VertexConsumer {
		private final List<Vertex> vertices;
		private @Nullable Vertex current;

		Recorder(List<Vertex> vertices) {
			this.vertices = vertices;
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			current = new Vertex(x, y);
			vertices.add(current);
			return this;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			return setColor(ARGB.color(a, r, g, b));
		}

		@Override
		public VertexConsumer setColor(int color) {
			if (current != null)
				current.color = color;
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			return this;
		}

		@Override
		public VertexConsumer setLineWidth(float width) {
			return this;
		}
	}

	/**
	 * A mask's render state, moved from its local space into the frame and recoloured from the
	 * element's {@link ColorField}. The mask's own alpha still applies, so a texture's transparent
	 * pixels stay transparent.
	 */
	private record RecoloredRenderState(GuiElementRenderState mask, Matrix3x2fc pose, ColorField field,
										@Nullable ScreenRectangle scissor) implements GuiElementRenderState {
		@Override
		public void buildVertices(VertexConsumer consumer) {
			mask.buildVertices(new VertexConsumer() {
				private final Vector2f local = new Vector2f();
				private final Vector2f framed = new Vector2f();

				@Override
				public VertexConsumer addVertex(float x, float y, float z) {
					local.set(x, y);
					pose.transformPosition(x, y, framed);
					consumer.addVertex(framed.x, framed.y, z);
					return this;
				}

				@Override
				public VertexConsumer setColor(int r, int g, int b, int a) {
					return setColor(ARGB.color(a, r, g, b));
				}

				@Override
				public VertexConsumer setColor(int color) {
					int sampled = field.sample(local.x, local.y);
					consumer.setColor(ARGB.color(ARGB.alpha(sampled) * ARGB.alpha(color) / 255, sampled));
					return this;
				}

				@Override
				public VertexConsumer setUv(float u, float v) {
					consumer.setUv(u, v);
					return this;
				}

				@Override
				public VertexConsumer setUv1(int u, int v) {
					consumer.setUv1(u, v);
					return this;
				}

				@Override
				public VertexConsumer setUv2(int u, int v) {
					consumer.setUv2(u, v);
					return this;
				}

				@Override
				public VertexConsumer setNormal(float x, float y, float z) {
					consumer.setNormal(x, y, z);
					return this;
				}

				@Override
				public VertexConsumer setLineWidth(float width) {
					consumer.setLineWidth(width);
					return this;
				}
			});
		}

		@Override
		public RenderPipeline pipeline() {
			return mask.pipeline();
		}

		@Override
		public TextureSetup textureSetup() {
			return mask.textureSetup();
		}

		@Override
		public @Nullable ScreenRectangle scissorArea() {
			return scissor;
		}

		@Override
		public @Nullable ScreenRectangle bounds() {
			ScreenRectangle local = mask.bounds();
			return local == null ? null : UIRenderHelper.getBounds(local, new Matrix3x2f(pose), scissor);
		}
	}
}
