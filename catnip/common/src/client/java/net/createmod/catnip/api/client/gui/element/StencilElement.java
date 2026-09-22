package net.createmod.catnip.api.client.gui.element;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws {@link #renderElement} only where {@link #renderStencil} draws.
 *
 * <h2>26.2 note</h2>
 * <p>There is no stencil buffer behind this any more; see {@link StencilMask} for how the mask is
 * applied, and why the old {@code prepareStencil}/{@code prepareElement}/{@code cleanUp} GL hooks
 * are gone.
 */
public interface StencilElement extends RenderElement {
	@Override
	default void submit(GuiGraphicsExtractor graphics) {
		graphics.pose().pushMatrix();
		transform(graphics);
		StencilMask.draw(graphics, this::renderStencil, this::renderElement);
		graphics.pose().popMatrix();
	}

	void renderStencil(GuiGraphicsExtractor graphics);

	void renderElement(GuiGraphicsExtractor graphics);

	default void transform(GuiGraphicsExtractor graphics) {
		graphics.pose().translate(getX(), getY());
	}
}
