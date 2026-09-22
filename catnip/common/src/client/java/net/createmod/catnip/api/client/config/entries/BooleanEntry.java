package net.createmod.catnip.api.client.config.entries;

import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.createmod.catnip.api.client.gui.element.RenderElement;
import net.createmod.catnip.api.client.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.api.client.gui.widget.BoxWidget;
import net.createmod.catnip.api.client.gui.texture.CatnipGuiTextures;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class BooleanEntry extends ValueEntry<Boolean> {

	RenderElement enabled;
	RenderElement disabled;
	BoxWidget button;

	public BooleanEntry(String label, ModConfigSpec.ConfigValue<Boolean> value, ModConfigSpec.ValueSpec spec, ModConfig.Type configType) {
		super(label, value, spec, configType);

		enabled = CatnipGuiTextures.ICON_CONFIRM.asStencil()
			.withElementRenderer((ms, width, height, alpha) -> UIRenderHelper.angledGradient(ms, 0, 0, height / 2, height, width, AbstractSimiWidget.COLOR_SUCCESS))
			.at(10, 0);

		disabled = CatnipGuiTextures.ICON_DISABLE.asStencil()
			.withElementRenderer((ms, width, height, alpha) -> UIRenderHelper.angledGradient(ms, 0, 0, height / 2, height, width, AbstractSimiWidget.COLOR_FAIL))
			.at(10, 0);

		button = new BoxWidget().showingElement(enabled)
			.withCallback(() -> setValue(!getValue()));

		listeners.add(button);
		onReset();
	}

	@Override
	protected void setEditable(boolean b) {
		super.setEditable(b);
		button.active = b;
	}

	@Override
	public void tick() {
		super.tick();
		button.tick();
	}

	@Override
	public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
		super.extractContent(graphics, mouseX, mouseY, isHovering, partialTick);

		button.setX(getX() + getWidth() - 80 - resetWidth);
		button.setY(getY() + 10);
		button.setWidth(35);
		button.setHeight(getHeight() - 20);
		button.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void onValueChange(Boolean newValue) {
		super.onValueChange(newValue);
		button.showingElement(newValue ? enabled : disabled);
		bumpCog(newValue ? 15f : -16f);
	}
}
