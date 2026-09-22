package net.createmod.catnip.api.client.gui.render.pip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;

import net.createmod.catnip.impl.client.mixin.PictureInPictureRendererAccessor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

/**
 * Blits a finished picture-in-picture texture with a linear filter instead of a nearest one.
 *
 * <h2>Why</h2>
 * <p>{@code PictureInPictureRenderer.blitTexture} samples with {@code FilterMode.NEAREST}, which
 * suits what vanilla draws this way -- a skin or a banner, blitted at the size it was drawn, where
 * every texel lands on a pixel. A machine drawn for a GUI is a model with edges at every angle, and
 * nearest sampling gives each screen pixel one whole texel: the diagonals come out as staircases
 * however large the texture is, because nothing is ever averaged. 1.21.1 had no intermediate texture
 * at all and drew these models straight into the framebuffer, so its edges were as smooth as the
 * geometry.
 *
 * <p>With {@code SUPERSAMPLE} in {@link GuiElementTransform} drawing at twice the size, a linear
 * filter averages those texels down and the edges come back. Clamped rather than repeating, so the
 * filter cannot pull in the opposite edge of the texture along the way.
 */
public final class SmoothPipBlit {

	private SmoothPipBlit() {}

	public static void blit(PictureInPictureRenderer<?> renderer, PictureInPictureRenderState renderState,
		GuiRenderState guiRenderState) {
		blit(renderer, renderState, guiRenderState, -1);
	}

	/**
	 * @param color multiplies the picture, alpha included; the pipeline is premultiplied, so a
	 *              translucent black darkens by the picture's own coverage
	 */
	public static void blit(PictureInPictureRenderer<?> renderer, PictureInPictureRenderState renderState,
		GuiRenderState guiRenderState, int color) {

		guiRenderState.addBlitToCurrentLayer(new BlitRenderState(
			RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
			TextureSetup.singleTexture(((PictureInPictureRendererAccessor) renderer).catnip$getTextureView(),
				RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
			renderState.pose(),
			renderState.x0(), renderState.y0(), renderState.x1(), renderState.y1(),
			0.0F, 1.0F, 1.0F, 0.0F,
			color,
			renderState.scissorArea(),
			null));
	}
}
