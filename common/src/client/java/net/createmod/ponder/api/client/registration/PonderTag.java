package net.createmod.ponder.api.client.registration;

import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.gui.element.ScreenElement;
import net.createmod.ponder.api.Ponder;
import net.createmod.ponder.api.client.PonderIndex;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class PonderTag implements ScreenElement {
	/**
	 * Highlight.ALL is a special PonderTag, used to indicate that all Tags
	 * for a certain Scene should be highlighted instead of selected single ones
	 */
	public static final class Highlight {
		public static final Identifier ALL = Ponder.id("_all");
	}

	private final Identifier id;
	@Nullable
	private final Identifier textureIconLocation;
	// 26.2 binds an item's default components long after tags are registered, so a tag remembers the
	// item it was given and only builds a stack from it when something asks to draw one.
	@Nullable
	private final ItemLike itemIcon;
	@Nullable
	private final ItemLike mainItem;
	@Nullable
	private ItemStack itemIconStack;
	@Nullable
	private ItemStack mainItemStack;


	public PonderTag(Identifier id, @Nullable Identifier textureIconLocation, @Nullable ItemLike itemIcon,
					 @Nullable ItemLike mainItem) {
		this.id = id;
		this.textureIconLocation = textureIconLocation;
		this.itemIcon = itemIcon;
		this.mainItem = mainItem;
	}

	public Identifier getId() {
		return id;
	}

	public ItemStack getMainItem() {
		if (mainItemStack == null)
			mainItemStack = mainItem == null ? ItemStack.EMPTY : new ItemStack(mainItem);
		return mainItemStack;
	}

	public ItemStack getItemIcon() {
		if (itemIconStack == null)
			itemIconStack = itemIcon == null ? ItemStack.EMPTY : new ItemStack(itemIcon);
		return itemIconStack;
	}

	public String getTitle() {
		return PonderIndex.getLangAccess().getTagName(id);
	}

	public String getDescription() {
		return PonderIndex.getLangAccess().getTagDescription(id);
	}

	public void render(GuiGraphicsExtractor graphics, int x, int y) {
		Matrix3x2fStack poseStack = graphics.pose();
		poseStack.pushMatrix();
		poseStack.translate(x, y);
		if (textureIconLocation != null) {
			poseStack.scale(0.25f, 0.25f);
			graphics.blit(RenderPipelines.GUI_TEXTURED, textureIconLocation, 0, 0, 0, 0, 0, 64, 64, 64, 64);
		} else if (!getItemIcon().isEmpty()) {
			GuiGameElement.of(getItemIcon())
				.scale(1.25f)
				.at(-2, -2)
				.submit(graphics);
		}
		poseStack.popMatrix();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (!(other instanceof PonderTag otherTag))
			return false;

		return getId().equals(otherTag.getId());
	}
}
