package net.createmod.catnip.impl.client.gui.element.pip;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.client.gui.render.pip.GuiBlockEntityRenderState;
import net.minecraft.client.Minecraft;
import net.createmod.catnip.api.client.gui.render.pip.SmoothPipBlit;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class GuiBlockEntityRenderer extends PictureInPictureRenderer<GuiBlockEntityRenderState> {
	@Override
	public Class<GuiBlockEntityRenderState> getRenderStateClass() {
		return GuiBlockEntityRenderState.class;
	}

	@Override
	protected void renderToTexture(GuiBlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
		renderState.transform().apply(poseStack, renderState.y0(), renderState.scale());

		CameraRenderState cameraRenderState = new CameraRenderState();

		Minecraft.getInstance().getBlockEntityRenderDispatcher()
			.getRenderer(renderState.blockEntityRenderState())
			.submit(renderState.blockEntityRenderState(), poseStack, submitNodeCollector, cameraRenderState);
	}

	@Override
	protected void blitTexture(GuiBlockEntityRenderState renderState, GuiRenderState guiRenderState) {
		SmoothPipBlit.blit(this, renderState, guiRenderState);
	}

	@Override
	protected String getTextureLabel() {
		return "catnip:gui_block_entity";
	}
}
