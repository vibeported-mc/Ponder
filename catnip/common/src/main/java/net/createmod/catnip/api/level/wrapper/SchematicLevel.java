package net.createmod.catnip.api.level.wrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.createmod.catnip.api.data.component.ComponentProcessors;
import net.createmod.catnip.api.math.BBHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;

public class SchematicLevel extends WrappedLevel implements ServerLevelAccessor, SchematicLevelAccessor {
	private static final Logger logger = LogUtils.getLogger();

	protected Map<BlockPos, BlockState> blocks;
	protected Map<BlockPos, BlockEntity> blockEntities;
	protected List<BlockEntity> renderedBlockEntities;
	protected List<Entity> entities;
	protected BoundingBox bounds;

	/**
	 * <h2>26.2 note</h2>
	 * <p>An entity's id is no longer a field that happens to be zero until something sets it --
	 * {@code Entity#getId} throws "Tried to access entity ID before ID assignment" while it still is.
	 * Renderers ask for it: an item entity's cluster render state seeds its layout from the id, so a
	 * ponder scene with a dropped item in it crashed the frame it was drawn. Nothing here is on a
	 * network, so any distinct non-zero number will do.
	 *
	 * <p>The constructor of {@code Entity} takes its id from {@link #getNextEntityId}, which a plain
	 * {@code Level} answers with zero. Answering here gives an id to every entity built in this level,
	 * including those a ponder element makes and draws itself without ever adding them -- the parrots
	 * and minecarts, whose render states ask for it all the same.
	 */
	private int nextEntityId = 1;

	public BlockPos anchor;
	public boolean renderMode;

	public SchematicLevel(Level original) {
		this(BlockPos.ZERO, original);
	}

	public SchematicLevel(BlockPos anchor, Level original) {
		super(original);
		setChunkSource(new SchematicChunkSource(this));
		this.blocks = new HashMap<>();
		this.blockEntities = new HashMap<>();
		this.bounds = new BoundingBox(BlockPos.ZERO);
		this.anchor = anchor;
		this.entities = new ArrayList<>();
		this.renderedBlockEntities = new ArrayList<>();
	}

	@Override
	public Set<BlockPos> getAllPositions() {
		return blocks.keySet();
	}

	@Override
	public boolean addFreshEntity(Entity entityIn) {
		if (entityIn instanceof ItemFrame itemFrame)
			itemFrame.setItem(ComponentProcessors.withUnsafeComponentsDiscarded(itemFrame.getItem()));
		if (entityIn instanceof ArmorStand armorStand)
			for (EquipmentSlot equipmentSlot : EquipmentSlot.values())
				armorStand.setItemSlot(equipmentSlot,
					ComponentProcessors.withUnsafeComponentsDiscarded(armorStand.getItemBySlot(equipmentSlot)));

		// Built in another level, an entity arrives with that level's id, or none.
		entityIn.setId(getNextEntityId());
		return entities.add(entityIn);
	}

	@Override
	public int getNextEntityId() {
		return nextEntityId++;
	}

	@Override
	public List<Entity> getEntityList() {
		return entities;
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		if (isOutsideBuildHeight(pos))
			return null;
		if (blockEntities.containsKey(pos))
			return blockEntities.get(pos);
		if (!blocks.containsKey(pos.subtract(anchor)))
			return null;

		BlockState blockState = getBlockState(pos);
		if (blockState.hasBlockEntity()) {
			try {
				BlockEntity blockEntity = ((EntityBlock) blockState.getBlock()).newBlockEntity(pos, blockState);
				if (blockEntity != null) {
					onBEadded(blockEntity, pos);
					blockEntities.put(pos, blockEntity);
					renderedBlockEntities.add(blockEntity);
				}
				return blockEntity;
			} catch (Exception e) {
				logger.debug("Could not create BlockEntity of block {}", blockState, e);
			}
		}
		return null;
	}

	protected void onBEadded(BlockEntity blockEntity, BlockPos pos) {
		blockEntity.setLevel(this);
	}

	@Override
	public BlockState getBlockState(BlockPos globalPos) {
		BlockPos pos = globalPos.subtract(anchor);

		if (pos.getY() - bounds.minY() == -1 && !renderMode)
			return Blocks.DIRT.defaultBlockState();
		if (getBounds().isInside(pos) && blocks.containsKey(pos))
			return processBlockStateForPrinting(blocks.get(pos));
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public Map<BlockPos, BlockState> getBlockMap() {
		return blocks;
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getBlockState(pos).getFluidState();
	}

	@Override
	public Holder<Biome> getBiome(BlockPos pos) {
		return level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
	}

	@Override
	public int getBrightness(LightLayer lightLayer, BlockPos pos) {
		return 15;
	}

	@Override
	public LevelTickAccess<Block> getBlockTicks() {
		return BlackholeTickAccess.emptyLevelList();
	}

	@Override
	public LevelTickAccess<Fluid> getFluidTicks() {
		return BlackholeTickAccess.emptyLevelList();
	}

	@Override
	public List<Entity> getEntities(Entity entity, AABB bb, Predicate<? super Entity> predicate) {
		return Collections.emptyList();
	}

	@Override
	public <T extends Entity> List<T> getEntitiesOfClass(Class<T> arg0, AABB arg1, Predicate<? super T> arg2) {
		return Collections.emptyList();
	}

	@Override
	public List<? extends Player> players() {
		return Collections.emptyList();
	}

	@Override
	public int getSkyDarken() {
		return 0;
	}

	@Override
	public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) {
		return predicate.test(getBlockState(pos));
	}

	@Override
	public boolean destroyBlock(BlockPos pos, boolean dropResources) {
		return setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
	}

	@Override
	public boolean removeBlock(BlockPos pos, boolean dropResources) {
		return setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
	}

	@Override
	public boolean setBlock(BlockPos pos, BlockState arg1, int arg2) {
		pos = pos.immutable()
			.subtract(anchor);
		bounds = BBHelper.encapsulate(bounds, pos);
		blocks.put(pos, arg1);
		if (blockEntities.containsKey(pos)) {
			BlockEntity blockEntity = blockEntities.get(pos);
			if (!blockEntity.getType()
				.isValid(arg1)) {
				blockEntities.remove(pos);
				renderedBlockEntities.remove(blockEntity);
			}
		}

		BlockEntity blockEntity = getBlockEntity(pos);
		if (blockEntity != null)
			blockEntities.put(pos, blockEntity);

		return true;
	}

	@Override
	public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
	}

	@Override
	public BoundingBox getBounds() {
		return bounds;
	}

	@Override
	public void setBounds(BoundingBox bounds) {
		this.bounds = bounds;
	}

	@Override
	public Iterable<BlockEntity> getBlockEntities() {
		return blockEntities.values();
	}

	@Override
	public Iterable<BlockEntity> getRenderedBlockEntities() {
		return renderedBlockEntities;
	}

	protected BlockState processBlockStateForPrinting(BlockState state) {
		if (state.getBlock() instanceof AbstractFurnaceBlock && state.hasProperty(BlockStateProperties.LIT))
			state = state.setValue(BlockStateProperties.LIT, false);
		return state;
	}

	@Override
	public ServerLevel getLevel() {
		if (this.level instanceof ServerLevel) {
			return (ServerLevel) this.level;
		}
		throw new IllegalStateException("Cannot use IServerWorld#getWorld in a client environment");
	}

	@Override
	public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
		return getLevel().getCurrentDifficultyAt(pos);
	}
}
