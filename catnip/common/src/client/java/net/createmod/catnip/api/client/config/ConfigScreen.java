package net.createmod.catnip.api.client.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.TriConsumer;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.animation.Force;
import net.createmod.catnip.api.animation.PhysicalFloat;
import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ConfigScreen extends AbstractSimiScreen {

	public static final Map<String, TriConsumer<Screen, GuiGraphicsExtractor, Float>> backgrounds = new HashMap<>();
	public static final PhysicalFloat cogSpin = PhysicalFloat.create().withLimit(10f).withDrag(0.3).addForce(new Force.Static(.2f));
	@Nullable
	public static String modID = null;
	@Nullable
	protected final Screen parent;

	public static BlockState shadowState = Blocks.POTTED_CRIMSON_ROOTS.defaultBlockState();
	/** How dark the spinning shadow behind every config screen is drawn. */
	public static int shadowColor = 0x60_000000;

	public ConfigScreen(@Nullable Screen parent) {
		super(Component.empty());
		this.parent = parent;
	}

	@Override
	public void tick() {
		super.tick();
		cogSpin.tick();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	protected void renderWindowBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		if (this.minecraft.level != null) {
			//in game
			graphics.fill(0, 0, this.width, this.height, 0xb0_282c34);
		} else {
			//in menus
			renderMenuBackground(graphics, partialTicks);
		}

		Matrix3x2fStack poseStack = graphics.pose();
		poseStack.pushMatrix();
		poseStack.translate(width * 0.5f, height * 0.5f);
		renderCog(graphics, partialTicks);
		poseStack.popMatrix();

		super.renderWindowBackground(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void renderWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		cogSpin.bump(3, -scrollY * 5);

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	public static String toHumanReadable(String key) {
		String s = key.replaceAll("_", " ");
		s = Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(s)).map(StringUtils::capitalize).collect(Collectors.joining(" "));
		s = StringUtils.normalizeSpace(s);
		return s;
	}

	/**
	 * By default, ConfigScreens will render the Vanilla Panorama as
	 * their background when not opened ingame.
	 * If your mod wants to render something else, please add to the
	 * {@code backgrounds} Map in this Class with your modID as the key.
	 */
	protected void renderMenuBackground(GuiGraphicsExtractor graphics, float partialTicks) {
		TriConsumer<Screen, GuiGraphicsExtractor, Float> customBackground = backgrounds.get(modID);
		if (customBackground != null) {
			customBackground.accept(this, graphics, partialTicks);
			return;
		}

		this.minecraft.gameRenderer.panorama().extractRenderState(graphics, this.width, this.height);

		graphics.fill(0, 0, this.width, this.height, 0x90_282c34);
	}

	/**
	 * The shadow of a slowly turning block behind the screen.
	 *
	 * <h2>26.2 note</h2>
	 * <p>1.21.1 drew the block into the stencil buffer and filled through it. A block in a 26.2 GUI
	 * is drawn offscreen and blitted in, and tinting that blit black does the same job: the result is
	 * the block's silhouette at {@link #shadowColor}.
	 */
	protected static void renderCog(GuiGraphicsExtractor graphics, float partialTicks) {
		Matrix3x2fStack poseStack = graphics.pose();
		poseStack.pushMatrix();

		poseStack.translate(-100, 100);
		poseStack.scale(200, 200);
		GuiGameElement.of(shadowState)
			.rotateBlock(22.5, cogSpin.getValue(partialTicks), 22.5)
			.tintResult(shadowColor)
			.submit(graphics);

		poseStack.popMatrix();
	}
}
