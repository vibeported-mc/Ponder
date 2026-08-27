package net.createmod.catnip.api.client;

import net.minecraft.client.KeyMapping;

/**
 * A key mapping that never counts as conflicting with another binding.
 * <p>
 * Mods bind modifier keys (shift, ctrl, alt) as mappings of their own so the key can be read by name
 * and rebound. Vanilla would then flag every ordinary binding on the same key as a conflict, which is
 * meaningless for a modifier. Reporting equality only against itself keeps those bindings quiet.
 */
public class ConflictSafeKeyMapping extends KeyMapping {

	public ConflictSafeKeyMapping(String name, int keysym, Category category) {
		super(name, keysym, category);
	}

	@Override
	public boolean same(KeyMapping other) {
		return this == other;
	}
}
