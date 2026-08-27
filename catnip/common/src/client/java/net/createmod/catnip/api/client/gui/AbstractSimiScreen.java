package net.createmod.catnip.api.client.gui;

import java.util.Collection;
import java.util.List;

import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.api.theme.Color;
import net.createmod.catnip.impl.client.mixin.ScreenAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * A screen laid out as a fixed-size window centred in the viewport.
 * <p>
 * Subclasses declare their window's size before {@code init()} and then draw relative to
 * {@link #guiLeft} / {@link #guiTop} inside {@link #renderWindow}, rather than working out their own
 * centring every frame.
 * <p>
 * 26.2 replaced immediate-mode screen rendering with an extraction pass, so the frame is built in
 * {@link #extractRenderState} and the hooks take a {@link GuiGraphicsExtractor}.
 */
public abstract class AbstractSimiScreen extends Screen implements CatnipScreenExtensions {
	protected static final Color BACKGROUND_COLOR = new Color(0x50_101010, true);

	protected int windowWidth, windowHeight;
	protected int windowXOffset, windowYOffset;
	protected int guiLeft, guiTop;

	protected AbstractSimiScreen(Component title) {
		super(title);
	}

	protected AbstractSimiScreen() {
		this(CommonComponents.EMPTY);
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowSize(int width, int height) {
		windowWidth = width;
		windowHeight = height;
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowOffset(int xOffset, int yOffset) {
		windowXOffset = xOffset;
		windowYOffset = yOffset;
	}

	@Override
	protected void init() {
		guiLeft = (width - windowWidth) / 2;
		guiTop = (height - windowHeight) / 2;
		guiLeft += windowXOffset;
		guiTop += windowYOffset;
	}

	@Override
	public void tick() {
		for (GuiEventListener listener : children()) {
			if (listener instanceof TickableGuiEventListener tickable) {
				tickable.tick();
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnE() {
		return true;
	}

	@SuppressWarnings("unchecked")
	protected <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(W... widgets) {
		for (W widget : widgets) {
			addRenderableWidget(widget);
		}
	}

	protected <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(Collection<W> widgets) {
		for (W widget : widgets) {
			addRenderableWidget(widget);
		}
	}

	protected void removeWidgets(GuiEventListener... widgets) {
		for (GuiEventListener widget : widgets) {
			removeWidget(widget);
		}
	}

	protected void removeWidgets(Collection<? extends GuiEventListener> widgets) {
		for (GuiEventListener widget : widgets) {
			removeWidget(widget);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		// The frame delta a screen is handed is not the accumulated tick fraction animations need.
		partialTicks = AnimationTickHolder.getGuiPartialTicks();
		Matrix3x2fStack ms = graphics.pose();

		ms.pushMatrix();

		prepareFrame();

		renderWindowBackground(graphics, mouseX, mouseY, partialTicks);
		renderWindow(graphics, mouseX, mouseY, partialTicks);

		for (Renderable renderable : getRenderables())
			renderable.extractRenderState(graphics, mouseX, mouseY, partialTicks);

		renderWindowForeground(graphics, mouseX, mouseY, partialTicks);

		endFrame();

		ms.popMatrix();
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		boolean keyPressed = super.keyPressed(keyEvent);
		if (keyPressed || getFocused() != null)
			return keyPressed;

		if (this.minecraft.options.keyInventory.matches(keyEvent)) {
			this.onClose();
			return true;
		}

		boolean consumed = false;

		for (GuiEventListener widget : children()) {
			if (widget instanceof AbstractSimiWidget simiWidget) {
				if (simiWidget.keyPressed(keyEvent))
					consumed = true;
			}
		}

		return consumed;
	}

	protected void prepareFrame() {
	}

	/**
	 * 26.2 extracts the screen background in its own stratum before this pass runs, so unlike the
	 * immediate-mode version this does nothing by default; override it to replace the background.
	 */
	protected void renderWindowBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
	}

	protected abstract void renderWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks);

	protected void renderWindowForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
	}

	protected void endFrame() {
	}

	@Deprecated
	protected void debugWindowArea(GuiGraphicsExtractor graphics) {
		graphics.fill(guiLeft + windowWidth, guiTop + windowHeight, guiLeft, guiTop, 0xD3D3D3D3);
	}

	protected final List<Renderable> getRenderables() {
		return ((ScreenAccessor) this).catnip$getRenderables();
	}

	@Override
	public @Nullable GuiEventListener getFocused() {
		GuiEventListener focused = super.getFocused();
		if (focused instanceof AbstractWidget && !focused.isFocused())
			focused = null;
		setFocused(focused);
		return focused;
	}
}
