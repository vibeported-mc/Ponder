package net.createmod.catnip.api.client.animation;

import java.util.ArrayList;
import java.util.List;

import net.createmod.catnip.api.client.level.wrapper.WrappedClientLevel;
import net.createmod.catnip.impl.client.mixin.TimerAccessor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.world.level.LevelAccessor;

public class AnimationTickHolder {
	private static int ticks;
	private static int pausedTicks;

	private static final List<AlternateClock> ALTERNATE_CLOCKS = new ArrayList<>();

	/**
	 * A clock some levels run on instead of the game's. A ponder scene is the case this exists for:
	 * its animations advance with the scene, not with the world the player left behind.
	 */
	public interface AlternateClock {
		boolean appliesTo(LevelAccessor level);

		int ticks();

		float partialTicks();
	}

	public static void registerAlternateClock(AlternateClock clock) {
		ALTERNATE_CLOCKS.add(clock);
	}

	public static void reset() {
		ticks = 0;
		pausedTicks = 0;
	}

	public static void tick() {
		if (!Minecraft.getInstance()
			.isPaused()) {
			ticks = (ticks + 1) % 1_728_000; // wrap around every 24 hours so we maintain enough floating point precision
		} else {
			pausedTicks = (pausedTicks + 1) % 1_728_000;
		}
	}

	public static int getTicks() {
		return getTicks(false);
	}

	public static int getTicks(boolean includePaused) {
		return includePaused ? ticks + pausedTicks : ticks;
	}

	public static int getTicks(LevelAccessor level) {
		if (level instanceof WrappedClientLevel wrapped)
			return getTicks(wrapped.getWrappedLevel());
		for (AlternateClock clock : ALTERNATE_CLOCKS)
			if (clock.appliesTo(level))
				return clock.ticks();
		return getTicks();
	}

	public static float getPartialTicks(LevelAccessor level) {
		if (level instanceof WrappedClientLevel wrapped)
			return getPartialTicks(wrapped.getWrappedLevel());
		for (AlternateClock clock : ALTERNATE_CLOCKS)
			if (clock.appliesTo(level))
				return clock.partialTicks();
		return getPartialTicks();
	}

	public static float getRenderTime() {
		return getTicks() + getPartialTicks();
	}

	public static float getRenderTime(LevelAccessor level) {
		return getTicks(level) + getPartialTicks(level);
	}

	/**
	 * @return the fraction between the current tick to the next tick, frozen during game pause [0-1]
	 */
	public static float getPartialTicks() {
		return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
	}

	/// In `Screen.render`, the partialTicks value is actually incorrect.
	///
	/// In other cases, like entity rendering, partialTicks is an accumulated fraction of ticks that have
	/// passed since the last game tick. It should range from 0-1, but may be larger during lag spikes.
	///
	/// `Screen.render` is instead given a simple frame delta, which is not very useful for smooth animations.
	/// The value will pretty much always be the same.
	///
	/// This method provides access to the accumulated delta. This is actually what vanilla
	/// does in [EnchantmentScreen], which needs a smooth animation for the book opening.
	///
	/// The accumulated delta is read straight off the timer rather than through
	/// [DeltaTracker#getGameTimeDeltaPartialTick], because that accessor hands back a value frozen at
	/// the moment of pause once the game is paused. Screens that animate are usually pause screens, so
	/// going through it would leave them stepping once per client tick instead of once per frame.
	public static float getGuiPartialTicks() {
		if (Minecraft.getInstance().getDeltaTracker() instanceof TimerAccessor timer)
			return timer.catnip$getDeltaTickResidual();

		return getPartialTicks();
	}
}
