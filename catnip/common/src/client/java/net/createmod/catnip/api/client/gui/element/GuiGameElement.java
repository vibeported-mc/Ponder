package net.createmod.catnip.api.client.gui.element;

import java.util.Objects;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.client.gui.ILightingSettings;
import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.createmod.catnip.api.client.gui.render.pip.GuiBlockEntityRenderState;
import net.createmod.catnip.api.client.gui.render.pip.GuiBlockModelRenderState;
import net.createmod.catnip.api.client.gui.render.pip.GuiElementTransform;
import net.createmod.catnip.api.client.gui.render.pip.GuiFluidStateRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class GuiGameElement {
    public static GuiRenderBuilder of(ItemStack stack) {
        return new GuiItemRenderBuilder(stack);
    }

    public static GuiRenderBuilder of(ItemLike itemProvider) {
        return new GuiItemRenderBuilder(itemProvider);
    }

    public static GuiRenderBuilder of(BlockState state) {
        return new GuiBlockStateRenderBuilder(state);
    }

    public static GuiRenderBuilder of(BlockState state, @Nullable BlockEntity blockEntity) {
        return new GuiBlockEntityRenderBuilder(state, blockEntity);
    }

    public static GuiRenderBuilder of(BlockEntity blockEntity) {
        return of(blockEntity.getBlockState(), blockEntity);
    }

    public static GuiRenderBuilder of(Fluid fluid) {
        return new GuiBlockStateRenderBuilder(
                fluid.defaultFluidState().createLegacyBlock().setValue(LiquidBlock.LEVEL, 0));
    }

    public abstract static class GuiRenderBuilder extends AbstractRenderElement {
        protected float xLocal, yLocal, zLocal;
        protected double xRot, yRot, zRot;
        protected double viewXRot, viewYRot, viewZRot;
        protected double scale = 1;
        protected int color = 0xFFFFFF;
        protected Vector2f rotationOffset = new Vector2f();
        protected float rotationOffsetZ;

        @Nullable
        protected ILightingSettings customLighting = null;

        public GuiRenderBuilder atLocal(double x, double y, double z) {
            this.xLocal = (float) x;
            this.yLocal = (float) y;
            this.zLocal = (float) z;
            return this;
        }

        /**
         * The orientation the whole scene is viewed from, applied before this element's own
         * rotation. Elements drawn together in one view share it.
         */
        public GuiRenderBuilder viewRotate(double xRot, double yRot, double zRot) {
            this.viewXRot = xRot;
            this.viewYRot = yRot;
            this.viewZRot = zRot;
            return this;
        }

        public GuiRenderBuilder rotate(double xRot, double yRot, double zRot) {
            this.xRot = xRot;
            this.yRot = yRot;
            this.zRot = zRot;
            return this;
        }

        public GuiRenderBuilder rotateBlock(double xRot, double yRot, double zRot) {
            return this.rotate(xRot, yRot, zRot)
                    .withRotationOffset(new Vector2f(0.5f, 0.5f));
        }

        public GuiRenderBuilder scale(double scale) {
            this.scale = scale;
            return this;
        }

        public GuiRenderBuilder color(int color) {
            this.color = color;
            return this;
        }

        public GuiRenderBuilder withRotationOffset(Vector2f offset) {
            this.rotationOffset = offset;
            return this;
        }

        /**
         * A pivot with depth, for elements whose own rotation is about a point away from the plane.
         */
        public GuiRenderBuilder withRotationOffset(float x, float y, float z) {
            this.rotationOffset = new Vector2f(x, y);
            this.rotationOffsetZ = z;
            return this;
        }

        public GuiRenderBuilder lighting(ILightingSettings lighting) {
            customLighting = lighting;
            return this;
        }

        protected void prepareMatrix(Matrix3x2fStack poseStack) {
            poseStack.pushMatrix();
            // RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            // RenderSystem.enableDepthTest();
            // RenderSystem.enableBlend();
            // RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
            prepareLighting();
        }

        /**
         * Only the element's place on the screen. Its position and orientation within the scene are
         * three-dimensional, which this stack no longer is, so they travel with the render state and
         * are applied while the element is drawn into its own texture.
         */
        protected void transformMatrix(Matrix3x2fStack poseStack) {
			poseStack.translate(x, y);
			poseStack.scale((float) scale, (float) scale);
        }

        protected GuiElementTransform elementTransform() {
            return new GuiElementTransform(xLocal, yLocal, zLocal,
                (float) viewXRot, (float) viewYRot, (float) viewZRot,
                (float) xRot, (float) yRot, (float) zRot,
                rotationOffset.x, rotationOffset.y, rotationOffsetZ);
        }

        protected void cleanUpMatrix(Matrix3x2fStack poseStack) {
            poseStack.popMatrix();
            cleanUpLighting();
        }

        protected void prepareLighting() {
            Objects.requireNonNullElse(customLighting, ILightingSettings.ITEMS_3D)
                    .apply();
        }

        protected void cleanUpLighting() {
            if (customLighting != null) {
                ILightingSettings.ITEMS_3D.apply();
            }
        }
    }

    protected static class GuiBlockModelRenderBuilder extends GuiRenderBuilder {
        protected BlockStateModel blockStateModel;
        protected BlockState blockState;

        @Nullable
        protected BlockEntity blockEntity;

        public GuiBlockModelRenderBuilder(
                BlockStateModel blockStateModel,
                @Nullable BlockState blockState,
                @Nullable BlockEntity blockEntity) {
            this.blockState = blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
            this.blockStateModel = blockStateModel;
            this.blockEntity = blockEntity;
        }

        @Override
        public void submit(GuiGraphicsExtractor graphics) {
			Matrix3x2fStack poseStack = graphics.pose();
			prepareMatrix(poseStack);
			transformMatrix(poseStack);

			submitModel(graphics);

			cleanUpMatrix(poseStack);
        }

        protected void submitModel(GuiGraphicsExtractor graphics) {
			graphics.guiRenderState.addPicturesInPictureState(
				new GuiBlockModelRenderState(blockState, blockEntity,
					new Matrix3x2f(graphics.pose()),
					elementTransform(),
					ARGB.color(255, color),
					0, 0, 16, 16, 1,
					null, null
				)
			);
        }
    }

    public static class GuiBlockEntityRenderBuilder extends GuiBlockModelRenderBuilder {
        public GuiBlockEntityRenderBuilder(
                BlockState blockState, @Nullable BlockEntity blockEntity) {
            super(Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState), blockState, blockEntity);
        }

        @Override
        protected void submitModel(GuiGraphicsExtractor graphics) {
            submitBlockEntity(graphics);

            super.submitModel(graphics);
        }

        private void submitBlockEntity(GuiGraphicsExtractor graphics) {
			if (blockEntity == null)
				return;

			BlockState stateBefore = blockEntity.getBlockState();
			blockEntity.setBlockState(this.blockState);
			net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState blockEntityRenderState = Minecraft.getInstance().getBlockEntityRenderDispatcher().tryExtractRenderState(blockEntity, Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks(), null, false);
			graphics.guiRenderState.addPicturesInPictureState(new GuiBlockEntityRenderState(blockEntityRenderState, new Matrix3x2f(graphics.pose()), elementTransform(), 0, 0, 16, 16, 1, null, null));
			blockEntity.setBlockState(stateBefore);
        }
    }

    public static class GuiBlockStateRenderBuilder extends GuiBlockModelRenderBuilder {
        public GuiBlockStateRenderBuilder(BlockState blockstate) {
            super(Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockstate), blockstate, null);
        }

        @Override
        protected void submitModel(GuiGraphicsExtractor graphics) {
            if (blockState.getBlock() instanceof BaseFireBlock) {
                ILightingSettings.ITEMS_FLAT.apply();
                super.submitModel(graphics);
                ILightingSettings.ITEMS_3D.apply();
                return;
            }

            super.submitModel(graphics);

            if (blockState.getFluidState().isEmpty()) return;

			graphics.guiRenderState.addPicturesInPictureState(new GuiFluidStateRenderState(blockState.getFluidState(), new Matrix3x2f(graphics.pose()),
				elementTransform(), 0, 0, 16, 16, 1, null, null));
        }
    }

    public static class GuiItemRenderBuilder extends GuiRenderBuilder {
        private final ItemStack stack;

        public GuiItemRenderBuilder(ItemStack stack) {
            this.stack = stack;
        }

        public GuiItemRenderBuilder(ItemLike provider) {
            this(new ItemStack(provider));
        }

        @Override
        public void submit(GuiGraphicsExtractor graphics) {
             Matrix3x2fStack poseStack = graphics.pose();
             prepareMatrix(poseStack);
             transformMatrix(poseStack);
			 graphics.item(this.stack, 0, 0);
             cleanUpMatrix(poseStack);
        }
    }
}
