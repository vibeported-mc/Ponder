package net.createmod.catnip.api.client.gui.render.pip;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.level.material.FluidState;

public record GuiFluidStateRenderState(
	FluidState fluidState,
	Matrix3x2f pose,
	GuiElementTransform transform,
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
