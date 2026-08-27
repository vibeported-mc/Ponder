package net.createmod.catnip.api.client.gui.render.pip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

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
	float xRotOffset, float yRotOffset, float zRotOffset
) {

	public static final GuiElementTransform NONE = new GuiElementTransform(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

	/**
	 * The view rotation goes on first, the way it used to be pushed onto the stack around a group of
	 * elements; the element's own rotation follows, about its rotation offset.
	 */
	public void apply(PoseStack poseStack) {
		poseStack.mulPose(Axis.ZP.rotationDegrees(viewZRot));
		poseStack.mulPose(Axis.XP.rotationDegrees(viewXRot));
		poseStack.mulPose(Axis.YP.rotationDegrees(viewYRot));

		poseStack.translate(xLocal, yLocal, zLocal);

		poseStack.translate(xRotOffset, yRotOffset, zRotOffset);
		poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
		poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
		poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
		poseStack.translate(-xRotOffset, -yRotOffset, -zRotOffset);
	}

}
