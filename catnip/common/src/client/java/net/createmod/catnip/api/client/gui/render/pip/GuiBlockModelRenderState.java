package net.createmod.catnip.api.client.gui.render.pip;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;

public record GuiBlockModelRenderState(
	BlockState state,
	/**
	 * The model to draw, which is not always the one the state would give.
	 *
	 * <p>An element built from a {@code BlockStateModel} -- one piece of a larger model, a cogwheel
	 * or a shaft on its own -- has no block state behind it, so the builder stands {@code AIR} in for
	 * one. The renderer used to ask the model set for the state's model and got the empty model that
	 * air has, which is why every partial model in a JEI recipe panel drew nothing at all. The model
	 * is chosen when the element is built, so it travels with the state.
	 */
	BlockStateModel model,
	@Nullable BlockEntity blockEntity,
	Matrix3x2f pose,
	GuiElementTransform transform,
	int color,
	/**
	 * Tints the finished picture as it is blitted into the GUI, -1 for none. Unlike {@link #color},
	 * which tints the model's faces, this applies to the rendered result, so a translucent black
	 * turns the model into a shadow of its own silhouette.
	 */
	int blitColor,
	int x0, int y0,
	int x1, int y1,
	float scale, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {

	/**
	 * The rectangle this element covers on screen.
	 *
	 * <h2>Why this is not the record's own field</h2>
	 * <p>{@code GuiRenderState.findAppropriateNode} asks every element for its bounds and
	 * <b>silently drops the ones that answer null</b> -- it returns false and the state is never
	 * added. Every caller here passes null, so nothing drawn through this state reached the screen
	 * at all: in JEI, every Create machine was missing from its recipe panel while the flat parts
	 * beside it, being ordinary blits with real bounds, drew normally.
	 *
	 * <p>Vanilla's own picture-in-picture states compute this in a convenience constructor, from
	 * screen coordinates they are given directly. These are built differently: {@code x0..y1} stay a
	 * local 0-16 box, because they also size the offscreen texture the model is drawn into, and the
	 * placement travels in {@link #pose()}. So the rectangle is that box put through the pose, which
	 * is what {@code transformMaxBounds} is for.
	 */
	@Override
	public ScreenRectangle bounds() {
		if (bounds != null) {
			return bounds;
		}

		ScreenRectangle onScreen = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
		return scissorArea != null ? scissorArea.intersection(onScreen) : onScreen;
	}
}
