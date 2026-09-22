package net.createmod.catnip.api.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;



import net.createmod.catnip.api.client.gui.element.BoxElement;
import net.createmod.catnip.api.client.gui.element.TextStencilElement;
import net.createmod.catnip.api.client.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.api.client.gui.widget.BoxWidget;
import net.createmod.catnip.api.client.platform.ModClientHooksHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public class ConfirmationScreen extends AbstractSimiScreen {
	private Screen source;
	private Consumer<Response> action = _success -> {
	};
	private final List<FormattedText> text = new ArrayList<>();
	private boolean centered = false;
	private int x;
	private int y;
	private int textWidth;
	private int textHeight;
	private boolean tristate;

	private BoxWidget confirm;
	private BoxWidget confirmDontSave;
	private BoxWidget cancel;
	private BoxElement textBackground;

	public enum Response {
		Confirm, ConfirmDontSave, Cancel
	}

	/*
	 * Removes text lines from the back of the list
	 * */
	public ConfirmationScreen removeTextLines(int amount) {
		if (amount > text.size())
			return clearText();

		text.subList(text.size() - amount, text.size()).clear();
		return this;
	}

	public ConfirmationScreen clearText() {
		this.text.clear();
		return this;
	}

	public ConfirmationScreen addText(FormattedText text) {
		this.text.add(text);
		return this;
	}

	public ConfirmationScreen withText(FormattedText text) {
		return clearText().addText(text);
	}

	public ConfirmationScreen at(int x, int y) {
		this.x = Math.max(x, 0);
		this.y = Math.max(y, 0);
		this.centered = false;
		return this;
	}

	public ConfirmationScreen centered() {
		this.centered = true;
		return this;
	}

	public ConfirmationScreen withAction(Consumer<Boolean> action) {
		this.action = r -> action.accept(r == Response.Confirm);
		return this;
	}

	public ConfirmationScreen withThreeActions(Consumer<Response> action) {
		this.action = action;
		this.tristate = true;
		return this;
	}

	/**
	 * <h2>26.2 note</h2>
	 * <p>1.21.1 swapped itself in as the current screen behind the game's back, so the source was
	 * neither closed nor re-initialised, and drew the source underneath itself every frame. NeoForge
	 * now has screen layers for exactly that: the source stays open beneath, and is drawn there.
	 */
	public void open(Screen source) {
		this.source = source;
		ModClientHooksHelper.INSTANCE.pushScreenLayer(this);
	}

	@Override
	public void tick() {
		super.tick();
		source.tick();
	}

	@Override
	protected void init() {
		super.init();

		ArrayList<FormattedText> copy = new ArrayList<>(text);
		text.clear();
		copy.forEach(t -> text.addAll(font.getSplitter().splitLines(t, 300, Style.EMPTY)));

		textHeight = text.size() * (font.lineHeight + 1) + 4;
		textWidth = 300;

		if (centered) {
			x = width / 2 - textWidth / 2 - 2;
			y = height / 2 - textHeight / 2 - 16;
		} else {
			x = Math.max(0, x - textWidth / 2);
			y = Math.max(0, y -= textHeight);
		}

		if (x + textWidth > width) {
			x = width - textWidth;
		}

		if (y + textHeight + 30 > height) {
			y = height - textHeight - 30;
		}

		int buttonX = x + textWidth / 2 - 6 - (int) (70 * (tristate ? 1.5f : 1));

		TextStencilElement confirmText =
			new TextStencilElement(font, Component.translatable(tristate ? "catnip.ui.save_label" : "catnip.ui.confirm_label")).centered(true, true);
		confirm = new BoxWidget(buttonX, y + textHeight + 6, 70, 16).withCallback(() -> accept(Response.Confirm));
		confirm.showingElement(confirmText.withElementRenderer(BoxWidget.gradientFactory.apply(confirm)));
		addRenderableWidget(confirm);

		buttonX += 12 + 70;

		if (tristate) {
			TextStencilElement confirmDontSaveText =
				new TextStencilElement(font, Component.translatable("catnip.ui.dont_save_label")).centered(true, true);
			confirmDontSave =
				new BoxWidget(buttonX, y + textHeight + 6, 70, 16).withCallback(() -> accept(Response.ConfirmDontSave));
			confirmDontSave.showingElement(
				confirmDontSaveText.withElementRenderer(BoxWidget.gradientFactory.apply(confirmDontSave)));
			addRenderableWidget(confirmDontSave);
			buttonX += 12 + 70;
		}

		TextStencilElement cancelText = new TextStencilElement(font, Component.translatable("catnip.ui.cancel_label")).centered(true, true);
		cancel = new BoxWidget(buttonX, y + textHeight + 6, 70, 16)
			.withCallback(() -> accept(Response.Cancel));
		cancel.showingElement(cancelText.withElementRenderer(BoxWidget.gradientFactory.apply(cancel)));
		addRenderableWidget(cancel);

		textBackground = new BoxElement()
			.withBackground(BoxElement.COLOR_BACKGROUND_FLAT)
			.gradientBorder(AbstractSimiWidget.COLOR_DISABLED)
			.withBounds(width + 10, textHeight + 35)
			.at(-5, y - 5);

		if (text.size() == 1)
			x = (width - font.width(text.get(0))) / 2;
	}

	@Override
	public void onClose() {
		accept(Response.Cancel);
	}

	private void accept(Response success) {
		ModClientHooksHelper.INSTANCE.popScreenLayer();
		action.accept(success);
	}

	@Override
	protected void renderWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		textBackground.submit(graphics);
		int offset = font.lineHeight + 1;
		int lineY = y - offset;

		for (FormattedText line : text) {
			lineY += offset;
			if (line == null)
				continue;
			graphics.text(font, line.getString(), x, lineY, 0xFFeaeaea, false);
		}
	}

	// the source is drawn beneath as a screen layer; this only dims it, without the menu blur
	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		graphics.fillGradient(0, 0, this.width, this.height, 0x70101010, 0x80101010);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}
