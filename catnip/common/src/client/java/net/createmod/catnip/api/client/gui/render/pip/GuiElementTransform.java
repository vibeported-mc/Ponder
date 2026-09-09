package net.createmod.catnip.api.client.gui.render.pip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.createmod.catnip.api.client.gui.ILightingSettings;

/**
 * How a game element sits inside its picture-in-picture texture.
 * <p>
 * Minecraft 26.2's GUI transform stack is two-dimensional, so an element's placement and orientation
 * in three dimensions can no longer be pushed onto it before drawing. It travels with the render
 * state instead and is applied while the element is rendered into its own texture.
 */
public record GuiElementTransform(
	float xLocal, float yLocal, float zLocal,
	float viewXRot, float viewYRot, float viewZRot,
	float xRot, float yRot, float zRot,
	float xRotOffset, float yRotOffset, float zRotOffset,
	/**
	 * The lighting the element is drawn under.
	 *
	 * <p>It has to travel with the state. 1.21.1 set it up in {@code prepareMatrix} and drew in the
	 * same breath, but a picture-in-picture element is only submitted then -- the drawing happens
	 * later in the frame, by which time whatever was set up has been replaced. Create's machines are
	 * built for {@code AnimatedKinetics.DEFAULT_LIGHTING} and came out dark under whatever they
	 * inherited instead. Vanilla's own picture-in-picture renderers set their lighting inside
	 * {@code renderToTexture} for the same reason.
	 */
	ILightingSettings lighting
) {


	/**
	 * How much room the element's box leaves around its anchor, in blocks.
	 *
	 * <p>The box sizes the texture the model is drawn into and the quad it is blitted across, and the
	 * model is clipped to it. 1.21.1 drew onto the screen with nothing to clip against, so a widget
	 * could put its parts anywhere around the anchor -- and they do: turning a block about the anchor
	 * alone swings it up to {@code sqrt(3)} blocks away, before a part's own offset. A box starting at
	 * the anchor cuts all that off, which left the toolbox showing one corner of itself.
	 *
	 * <p>Four blocks, because the offsets go further than they look: the crushing wheels sit two blocks
	 * apart, so with the far wheel's own block and its swing the scene reaches about three and a half
	 * blocks out, and at two it lost the second wheel down its middle. Widening this costs no
	 * resolution -- {@link #unitsPerBlock} is what decides that -- only the size of the texture.
	 */
	public static final int BLOCKS_OF_ROOM = 2;

	/**
	 * What the crushing wheels need: they sit two blocks apart, so with the far wheel's own block and
	 * its swing the scene reaches about three and a half blocks from the anchor.
	 */
	public static final int WIDE_ROOM = 4;

	/**
	 * How many box units a block spans, which is what decides the element's resolution.
	 *
	 * <p>The texture is the box in units times the GUI scale, while the quad it lands on is however
	 * many screen pixels the caller asked for. Pinning this at sixteen gave a block
	 * {@code 16 * guiScale} texels however big it was drawn, so anything past that -- the toolbox at
	 * fifty pixels a block -- was magnified from too few and came out soft. Enough units that a block
	 * gets at least one texel per pixel it covers, and never fewer than sixteen.
	 */
	public static int unitsPerBlock(double scale) {
		return Math.max(16, net.minecraft.util.Mth.ceil(SUPERSAMPLE * scale));
	}

	/**
	 * How many texels the element is drawn with for each pixel it covers.
	 *
	 * <p>The texture is the box in units times the GUI scale, and the quad it lands on is that same box
	 * through a pose scaled by {@code scale / unitsPerBlock} -- so the GUI scale is in both and cancels,
	 * and a block gets {@code unitsPerBlock / scale} texels per pixel. At the sixteen this was pinned
	 * at, the toolbox at fifty pixels a block was drawn from a third of the texels it covered, which is
	 * the chunkiness; dividing by the GUI scale here only ever brought that back to parity. Twice the
	 * units means twice the texels each way, which the linear blit then averages down.
	 */
	private static final int SUPERSAMPLE = 2;

	public static int boxMin(double scale) {
		return boxMin(scale, BLOCKS_OF_ROOM);
	}

	public static int boxMax(double scale) {
		return boxMax(scale, BLOCKS_OF_ROOM);
	}

	public static int boxMin(double scale, int blocksOfRoom) {
		return -blocksOfRoom * unitsPerBlock(scale);
	}

	public static int boxMax(double scale, int blocksOfRoom) {
		return blocksOfRoom * unitsPerBlock(scale);
	}

	public static final GuiElementTransform NONE = new GuiElementTransform(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ILightingSettings.ITEMS_3D);

	/**
	 * Places the element inside its picture-in-picture texture, the way 1.21.1's
	 * {@code GuiGameElement.transformMatrix} placed it on the GUI pose stack.
	 *
	 * <p>The steps after the view rotation are that method's, in its order: the local offset, the
	 * flip, then the element's own rotation about its offset. The two corrections at the top are for
	 * where 26.2 leaves us before we start.
	 */
	/**
	 * Only the part that puts the pass in the element's own space: its lighting, the depth mirror and
	 * the anchor. A widget drawing several parts into one pass does the rest itself, per part, so that
	 * each one gets 1.21.1's order -- offset, flip, then its own rotation -- rather than sharing a
	 * single flip that would fall on the wrong side of the offsets.
	 */
	public void applyAnchorOnly(PoseStack poseStack, int y0, float unitsPerBlock) {
		lighting.apply();
		poseStack.scale(1, 1, -1);
		// prepare() starts the origin at the box's bottom edge; the anchor is however much room the box
		// was given back up from it, which is what y0 records.
		poseStack.translate(0, y0 / unitsPerBlock, 0);
	}

	/**
	 * {@code UIRenderHelper.flipForGuiRender} on 1.21.1, for a widget composing its own parts.
	 */
	public static void flipForGuiRender(PoseStack poseStack) {
		poseStack.scale(1, -1, 1);
	}

	public void apply(PoseStack poseStack, int y0, float unitsPerBlock) {
		lighting.apply();

		// Undo the depth mirror in PictureInPictureRenderer.prepare, which scales by (s, s, -s).
		// 1.21.1 drew these models on the GUI's pose stack, which negated nothing: with its Y-down
		// projection and the flip below, that came out right-handed. The extra negation here makes it
		// left-handed, so faces wind backwards -- the far side of a block is drawn over the near side,
		// which is what the millstone's stone base shows. Vanilla's own picture-in-picture content is
		// authored for that space; a block model is not.
		poseStack.scale(1, 1, -1);

		// Put the origin back on the element's anchor. PictureInPictureRenderer.prepare starts it at
		// the middle of the texture horizontally and its bottom edge vertically, neither of which moves
		// with the box; 1.21.1 anchored the element at its own x and y. For the default box that starts
		// at the anchor this is half a block left and nothing vertically, which is what it used to be.
		poseStack.translate(0, y0 / unitsPerBlock, 0);

		// From here down this is GuiGameElement.transformMatrix on 1.21.1, in its order. The scene's
		// angle came off the pose stack there, pushed once around a group of elements; this stack is
		// two-dimensional and cannot hold it, so each element carries its own copy.
		poseStack.mulPose(Axis.ZP.rotationDegrees(viewZRot));
		poseStack.mulPose(Axis.XP.rotationDegrees(viewXRot));
		poseStack.mulPose(Axis.YP.rotationDegrees(viewYRot));

		poseStack.translate(xLocal, yLocal, zLocal);

		// UIRenderHelper.flipForGuiRender. A block model is world geometry with Y up and the GUI's
		// projection has Y down, exactly as on 1.21.1; the picture-in-picture blit does not undo it,
		// which is plain in the millstone, whose view angles are all zero -- with no flip here it draws
		// wooden rim down and stone up, the wrong way round.
		poseStack.scale(1, -1, 1);

		poseStack.translate(xRotOffset, yRotOffset, zRotOffset);
		poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
		poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
		poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
		poseStack.translate(-xRotOffset, -yRotOffset, -zRotOffset);
	}

}
