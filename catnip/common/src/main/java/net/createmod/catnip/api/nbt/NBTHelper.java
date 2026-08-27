package net.createmod.catnip.api.nbt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import net.createmod.catnip.api.data.codec.CatnipCodecUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.FloatTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Unit;

// TODO - Everything here needs to be rethought with how codecs exist now and should be used everywhere they can
@Deprecated(forRemoval = true)
public class NBTHelper {
	public static void putMarker(CompoundTag nbt, String marker) {
		nbt.store(marker, Unit.CODEC, Unit.INSTANCE);
	}

	// Backwards compatible with 1.20
	public static BlockPos readBlockPos(CompoundTag nbt, String key) {
		Optional<BlockPos> pos = nbt.read(key, BlockPos.CODEC);
		if (pos.isPresent())
			return pos.get();
		CompoundTag oldTag = nbt.getCompoundOrEmpty(key);
		return new BlockPos(
			oldTag.getIntOr("X", 0),
			oldTag.getIntOr("Y", 0),
			oldTag.getIntOr("Z", 0)
		);
	}

	public static <T> ListTag writeCompoundList(Iterable<T> list, Function<T, CompoundTag> serializer) {
		ListTag listNBT = new ListTag();
		list.forEach(t -> {
			CompoundTag apply = serializer.apply(t);
			if (apply == null)
				return;
			listNBT.add(apply);
		});
		return listNBT;
	}

	public static <T> List<T> readCompoundList(ListTag listNBT, Function<CompoundTag, T> deserializer) {
		List<T> list = new ArrayList<>(listNBT.size());
		listNBT.forEach(inbt -> list.add(deserializer.apply((CompoundTag) inbt)));
		return list;
	}

	public static void iterateCompoundList(ListTag listNBT, Consumer<CompoundTag> consumer) {
		listNBT.forEach(inbt -> consumer.accept((CompoundTag) inbt));
	}

	/**
	 * Enum, item list, AABB and identifier helpers, restored for downstream mods that still call
	 * them. They store the same shapes as before so existing saves keep loading.
	 */
	public static <T extends Enum<?>> T readEnum(CompoundTag nbt, String key, Class<T> enumClass) {
		T[] enumConstants = enumClass.getEnumConstants();
		if (enumConstants == null)
			throw new IllegalArgumentException("Non-Enum class passed to readEnum: " + enumClass.getName());
		String name = nbt.getStringOr(key, "");
		for (T t : enumConstants) {
			if (t.name()
				.equals(name))
				return t;
		}
		return enumConstants[0];
	}

	public static <T extends Enum<?>> void writeEnum(CompoundTag nbt, String key, T enumConstant) {
		nbt.putString(key, enumConstant.name());
	}

	public static ListTag writeItemList(Iterable<ItemStack> stacks, HolderLookup.Provider registries) {
		ListTag listNBT = new ListTag();
		for (ItemStack stack : stacks)
			CatnipCodecUtils.encode(ItemStack.CODEC, registries, stack).ifPresent(listNBT::add);
		return listNBT;
	}

	public static List<ItemStack> readItemList(ListTag stacks, HolderLookup.Provider registries) {
		List<ItemStack> list = new ArrayList<>();
		for (int i = 0; i < stacks.size(); i++)
			CatnipCodecUtils.decode(ItemStack.CODEC, registries, stacks.getCompoundOrEmpty(i)).ifPresent(list::add);
		return list;
	}

	public static ListTag writeAABB(AABB bb) {
		ListTag bbtag = new ListTag();
		bbtag.add(FloatTag.valueOf((float) bb.minX));
		bbtag.add(FloatTag.valueOf((float) bb.minY));
		bbtag.add(FloatTag.valueOf((float) bb.minZ));
		bbtag.add(FloatTag.valueOf((float) bb.maxX));
		bbtag.add(FloatTag.valueOf((float) bb.maxY));
		bbtag.add(FloatTag.valueOf((float) bb.maxZ));
		return bbtag;
	}

	public static @Nullable AABB readAABB(ListTag bbTag) {
		if (bbTag.isEmpty())
			return null;
		return new AABB(bbTag.getFloatOr(0, 0), bbTag.getFloatOr(1, 0), bbTag.getFloatOr(2, 0),
			bbTag.getFloatOr(3, 0), bbTag.getFloatOr(4, 0), bbTag.getFloatOr(5, 0));
	}

	public static void writeIdentifier(CompoundTag nbt, String key, Identifier identifier) {
		nbt.putString(key, identifier.toString());
	}

	public static Identifier readIdentifier(CompoundTag nbt, String key) {
		return Identifier.parse(nbt.getStringOr(key, ""));
	}

	public static Tag getINBT(CompoundTag nbt, String id) {
		Tag inbt = nbt.get(id);
		if (inbt != null)
			return inbt;
		return new CompoundTag();
	}

	public static CompoundTag intToCompound(int i) {
		CompoundTag compoundTag = new CompoundTag();
		compoundTag.putInt("V", i);
		return compoundTag;
	}

	public static int intFromCompound(CompoundTag compoundTag) {
		return compoundTag.getIntOr("V", 0);
	}
}
