package net.createmod.catnip.impl.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;

/**
 * The texture a picture-in-picture pass drew into, so that a renderer can blit it with a filter of
 * its own choosing rather than the nearest-neighbour one vanilla hard-codes.
 */
@Mixin(PictureInPictureRenderer.class)
public interface PictureInPictureRendererAccessor {
	@Accessor("textureView")
	GpuTextureView catnip$getTextureView();
}
