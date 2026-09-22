package net.createmod.catnip.api.client.config.entries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import net.createmod.catnip.api.client.config.ConfigAnnotations;
import net.createmod.catnip.api.client.config.ConfigScreen;
import net.createmod.catnip.api.client.gui.element.DelegatedStencilElement;
import net.createmod.catnip.api.client.gui.widget.BoxWidget;
import net.createmod.catnip.api.client.lang.FontHelper;
import net.createmod.catnip.api.client.lang.FontHelper.Palette;
import net.createmod.catnip.api.config.ConfigHelper;
import net.createmod.catnip.api.data.Pair;
import net.createmod.catnip.api.client.config.ConfigScreenList;
import net.createmod.catnip.api.client.gui.texture.CatnipGuiTextures;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ValueEntry<T> extends ConfigScreenList.LabeledEntry {

	protected static final int resetWidth = 28;//including 6px offset on either side

	public static final ClipboardManager clipboardHelper = new ClipboardManager();

	protected ModConfigSpec.ConfigValue<T> value;
	protected ModConfigSpec.ValueSpec spec;
	protected BoxWidget resetButton;
	protected boolean editable = true;
	protected final ModConfig.Type type;

	public ValueEntry(String label, ModConfigSpec.ConfigValue<T> value, ModConfigSpec.ValueSpec spec, ModConfig.Type type) {
		super(label);
		this.value = value;
		this.spec = spec;
		this.type = type;
		this.path = String.join(".", value.getPath());

		resetButton = new BoxWidget(0, 0, resetWidth - 12, 16)
			.showingElement(CatnipGuiTextures.ICON_CONFIG_RESET.asStencil())
			.withCallback(() -> {
				setValue((T) spec.getDefault());
				this.onReset();
			});
		resetButton.modifyElement(e -> ((DelegatedStencilElement) e).withElementRenderer(BoxWidget.gradientFactory.apply(resetButton)));

		listeners.add(resetButton);

		List<String> path = value.getPath();
		labelTooltip.add(Component.literal(label).withStyle(ChatFormatting.WHITE));
		String comment = spec.getComment();
		if (comment == null || comment.isEmpty())
			return;

		List<String> commentLines = new ArrayList<>(Arrays.asList(comment.split("\n")));


		Pair<String, Map<String, String>> metadata = ConfigHelper.readMetadataFromComment(commentLines);
		if (metadata.getFirst() != null) {
			unit = metadata.getFirst();
		}
		if (metadata.getSecond() != null && !metadata.getSecond().isEmpty()) {
			annotations.putAll(metadata.getSecond());
		}
		// add comment to tooltip
		labelTooltip.addAll(commentLines.stream()
			.filter(s -> !s.startsWith("Range"))
			.map(s -> s.equals(".") ? " " : s)
			.map(Component::literal)
			.flatMap(stc -> FontHelper.cutTextComponent(stc, Palette.ALL_GRAY).stream())
			.toList()
		);

		if (annotations.containsKey(ConfigAnnotations.RequiresRelog.TRUE.getName()))
			labelTooltip.addAll(FontHelper.cutTextComponent(Component.translatable("catnip.ui.value_entry.relog_required"), Palette.GRAY_AND_GOLD));

		if (annotations.containsKey(ConfigAnnotations.RequiresRestart.CLIENT.getName()))
			labelTooltip.addAll(FontHelper.cutTextComponent(Component.translatable("catnip.ui.value_entry.restart_required"), Palette.GRAY_AND_RED));

		String fullPath = ConfigScreen.modID + ":" + type.extension() + "." + String.join(".", path);
		Font font = Minecraft.getInstance().font;
		if (font.width(fullPath) > FontHelper.MAX_WIDTH_PER_LINE) {
			int trimPos = 0;
			while (trimPos < fullPath.length() && font.width("..." + fullPath.substring(trimPos)) > FontHelper.MAX_WIDTH_PER_LINE)
				trimPos++;
			fullPath = "..." + fullPath.substring(trimPos);
		}
		labelTooltip.add(Component.literal(fullPath).withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	protected void setEditable(boolean b) {
		editable = b;
		resetButton.active = editable && !isCurrentValueDefault();
		resetButton.animateGradientFromState();
	}

	@Override
	public void tick() {
		super.tick();
		resetButton.tick();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick))
			return true;

		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return false;
		}

		Window window = Minecraft.getInstance().getWindow();
		if (!InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)) {
			return false;
		}

		// ctrl-click to copy the full path to clipboard
		this.annotations.put("highlight", ":)");
		clipboardHelper.setClipboard(window, ConfigScreen.modID + ":" + type.extension() + "." + path);

		return true;
	}

	@Override
	public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
		super.extractContent(graphics, mouseX, mouseY, isHovering, partialTick);

		resetButton.setX(getX() + getWidth() - resetWidth + 6);
		resetButton.setY(getY() + 10);
		resetButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	protected int getLabelWidth(int totalWidth) {
		return (int) (totalWidth * labelWidthMult) + 30;
	}

	public void setValue(@NonNull T value) {
		ConfigHelper.setValue(path, this.value, value, annotations);
		onValueChange(value);
	}

	@NonNull
	public T getValue() {
		return ConfigHelper.getValue(path, this.value);
	}

	protected boolean isCurrentValueDefault() {
		return spec.getDefault().equals(getValue());
	}

	public void onReset() {
		onValueChange(getValue());
	}

	public void onValueChange() {
		onValueChange(getValue());
	}

	public void onValueChange(T newValue) {
		resetButton.active = editable && !isCurrentValueDefault();
		resetButton.animateGradientFromState();
	}

	protected void bumpCog() {
		bumpCog(10f);
	}

	protected void bumpCog(float force) {
		ConfigScreen.cogSpin.bump(3, force);
	}
}
