package net.createmod.catnip.api.client.render;

import java.util.function.BiFunction;

import net.createmod.catnip.api.Catnip;
import net.createmod.catnip.api.client.gui.texture.CatnipSpecialTextures;
import net.createmod.catnip.impl.client.mixin.RenderTypeAccessor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public abstract class PonderRenderTypes {
	private static final RenderType GUI = RenderTypeAccessor.catnip$create(
		createLayerName("gui"),
		RenderSetup.builder(RenderPipelines.GUI)
			.createRenderSetup()
	);

	private static final RenderType OUTLINE_SOLID = RenderTypeAccessor.catnip$create(
		createLayerName("outline_solid"),
		RenderSetup.builder(RenderPipelines.ENTITY_SOLID)
			.withTexture("Sampler0", CatnipSpecialTextures.BLANK.getId())
			.useLightmap()
			.useOverlay()
			.createRenderSetup()
	);

	private static final BiFunction<Identifier, Boolean, RenderType> OUTLINE_TRANSLUCENT = Util.memoize((texture, cull) ->
		RenderTypeAccessor.catnip$create(
			createLayerName("outline_translucent" + (cull ? "_cull" : "")),
			RenderSetup.builder(cull ? RenderPipelines.ENTITY_TRANSLUCENT_CULL : RenderPipelines.ENTITY_TRANSLUCENT)
				.withTexture("Sampler0", texture)
				.sortOnUpload()
				.useLightmap()
				.useOverlay()
				.setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
				.createRenderSetup()
		)
	);

	public static RenderType gui() {
		return GUI;
	}

	public static RenderType outlineSolid() {
		return OUTLINE_SOLID;
	}

	public static RenderType outlineTranslucent(Identifier texture, boolean cull) {
		return OUTLINE_TRANSLUCENT.apply(texture, cull);
	}

	private static String createLayerName(String name) {
		return Catnip.ID + ":" + name;
	}
}
