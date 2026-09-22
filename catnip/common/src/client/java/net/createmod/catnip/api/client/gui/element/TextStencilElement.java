package net.createmod.catnip.api.client.gui.element;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix3x2fStack;

import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;

public class TextStencilElement extends DelegatedStencilElement {
	protected Font font;
	protected MutableComponent component = Component.empty();
	protected boolean centerVertically = false;
	protected boolean centerHorizontally = false;

	public TextStencilElement(Font font) {
		super();
		this.font = font;
		height = 10;
	}

	public TextStencilElement(Font font, String text) {
		this(font);
		component = Component.literal(text);
	}

	public TextStencilElement(Font font, MutableComponent component) {
		this(font);
		this.component = component;
	}

	public TextStencilElement withText(String text) {
		component = Component.literal(text);
		return this;
	}

	public TextStencilElement withText(MutableComponent component) {
		this.component = component;
		return this;
	}

	public TextStencilElement centered(boolean vertical, boolean horizontal) {
		this.centerVertically = vertical;
		this.centerHorizontally = horizontal;
		return this;
	}

	/**
	 * <h2>26.2 note</h2>
	 * <p>Text is drawn late, from a {@code GuiTextRenderState} whose glyphs only exist once the frame
	 * is prepared, so it cannot be recoloured vertex by vertex the way {@link StencilMask} recolours
	 * other masks. Each character takes the element's colour at its own centre instead, which is
	 * what the stencil showed through a glyph that narrow.
	 */
	@Override
	public void submit(GuiGraphicsExtractor graphics) {
		Matrix3x2fStack poseStack = graphics.pose();
		poseStack.pushMatrix();
		transform(graphics);

		StencilMask.ColorField field = StencilMask.ColorField.of(StencilMask.capture(this::renderElement));
		if (!field.isEmpty()) {
			float x = textX(), y = textY();
			float centreY = y + font.lineHeight / 2f;
			StringSplitter splitter = font.getSplitter();

			List<Glyph> glyphs = new ArrayList<>();
			float[] advance = {x};
			int[] alpha = {0};
			component.getVisualOrderText().accept((index, style, codepoint) -> {
				float width = splitter.stringWidth(FormattedCharSequence.codepoint(codepoint, style));
				int colour = field.sample(advance[0] + width / 2, centreY);
				alpha[0] = Math.max(alpha[0], ARGB.alpha(colour));
				glyphs.add(new Glyph(index, style.withColor(colour & 0xFFFFFF), codepoint));
				advance[0] += width;
				return true;
			});

			FormattedCharSequence coloured = sink -> {
				for (Glyph c : glyphs)
					if (!sink.accept(c.index, c.style, c.codepoint))
						return false;
				return true;
			};
			graphics.text(font, coloured, Math.round(x), Math.round(y), ARGB.color(alpha[0], 0xFFFFFF), false);
		}

		poseStack.popMatrix();
	}

	private record Glyph(int index, Style style, int codepoint) {}

	private float textX() {
		return centerHorizontally ? width / 2f - font.width(component) / 2f : 0;
	}

	private float textY() {
		return centerVertically ? height / 2f - (font.lineHeight - 1) / 2f : 0;
	}

	@Override
	public void renderStencil(GuiGraphicsExtractor graphics) {
		graphics.text(font, component, Math.round(textX()), Math.round(textY()), Color.BLACK.getRGB(), false);
	}

	@Override
	public void renderElement(GuiGraphicsExtractor graphics) {
		Matrix3x2fStack poseStack = graphics.pose();
		poseStack.pushMatrix();
		poseStack.translate(textX(), textY());
		element.render(graphics, font.width(component), font.lineHeight + 2, alpha);
		poseStack.popMatrix();
	}

	public MutableComponent getComponent() {
		return component;
	}
}
