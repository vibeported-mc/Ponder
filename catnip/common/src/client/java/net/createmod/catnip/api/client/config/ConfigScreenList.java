package net.createmod.catnip.api.client.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.createmod.catnip.api.client.config.ConfigAnnotations.RequiresRelog;
import net.createmod.catnip.api.client.config.ConfigAnnotations.RequiresRestart;
import net.createmod.catnip.api.client.gui.TickableGuiEventListener;
import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.createmod.catnip.api.client.gui.element.TextStencilElement;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ConfigScreenList extends ObjectSelectionList<ConfigScreenList.Entry> implements TickableGuiEventListener {

	@Nullable
	public static EditBox currentText;

	/** What the list shows with no search: this menu's own entries. */
	@Nullable
	public List<Entry> allEntries;

	/** What a search looks through: every value in this menu and the menus below it. */
	@Nullable
	public List<Entry> deepEntries;

	public ConfigScreenList(Minecraft client, int width, int height, int top, int elementHeight) {
		super(client, width, height, top, elementHeight);
		currentText = null;
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		Color c = new Color(0x60_000000);
		UIRenderHelper.angledGradient(graphics, 90, getX() + width / 2, getY(), width, 5, c, Color.TRANSPARENT_BLACK);
		UIRenderHelper.angledGradient(graphics, -90, getX() + width / 2, getBottom(), width, 5, c, Color.TRANSPARENT_BLACK);
		UIRenderHelper.angledGradient(graphics, 0, getX(), getY() + height / 2, height, 5, c, Color.TRANSPARENT_BLACK);
		UIRenderHelper.angledGradient(graphics, 180, getRight(), getY() + height / 2, height, 5, c, Color.TRANSPARENT_BLACK);

		// the scissor this sets up clips the entries themselves, and 26.2 applies it per render
		// state, so neither the old manual GL scissor nor the flush before it is needed
		super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	// Catnip draws its own shading above; not vanilla's list texture and separators
	@Override
	protected void extractListBackground(GuiGraphicsExtractor graphics) {}

	@Override
	protected void extractListSeparators(GuiGraphicsExtractor graphics) {}

	@Override
	public int getRowWidth() {
		return width - 16;
	}

	@Override
	protected int scrollBarX() {
		return getX() + this.width - 6;
	}

	@Override
	public void tick() {
		children().forEach(Entry::tick);
	}

	/**
	 * 26.2 hands out {@link #children()} read-only, so entries go in through the list, which also
	 * positions them.
	 */
	public void addConfigEntry(Entry entry) {
		addEntry(entry);
	}

	@Override
	public void clearEntries() {
		super.clearEntries();
	}

	public void sortEntries(Comparator<Entry> comparator) {
		sort(comparator);
	}

	public boolean search(@Nullable String query) {
		clearEntries();
		setScrollAmount(0);

		if (query == null || query.trim().isEmpty()) {
			if (allEntries != null)
				allEntries.forEach(this::addEntry);
			return true;
		}

		List<Entry> source =
			deepEntries != null ? deepEntries
			: allEntries != null ? allEntries
			: List.of();

		String q = query.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
		List<Entry> searchResults = source.stream()
			.filter(entry -> entry.path != null)
			.map(entry -> {
				String[] parts = entry.path.split("\\.");
				String key = parts[parts.length - 1].toLowerCase(Locale.ROOT);
				float distance = relevanceScore(q, key);
				return Map.entry(entry, distance);
			})
			.filter(map -> map.getValue() <= 0.8)
			.sorted(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey)
			.toList();

		if (searchResults.isEmpty()) {
			return false;
		}

		searchResults.forEach(this::addEntry);

		return true;
	}

	private static float relevanceScore(String query, String target) {
		int m = query.length();
		int n = target.length();
		int[][] table = new int[m + 1][n + 1];

		// Levenshtein Distance Algorithm
		// First row stays 0: no cost to skip leading target characters,
		// allowing the query to match against any substring of the target.
		for (int i = 0; i <= m; i++) table[i][0] = i;

		for (int i = 1; i <= m; i++) {
			for (int j = 1; j <= n; j++) {
				if (query.charAt(i - 1) == target.charAt(j - 1)) {
					table[i][j] = table[i - 1][j - 1];
				} else {
					table[i][j] = Math.min(table[i - 1][j - 1], Math.min(
						table[i][j - 1],
						table[i - 1][j]
					)) + 1;
				}
			}
		}

		// Minimum over all end positions: best substring match within target
		int rawDistance = Integer.MAX_VALUE;
		for (int j = 0; j <= n; j++) {
			rawDistance = Math.min(rawDistance, table[m][j]);
		}

		// Reject matches that exceed the maximum allowed edits for this query length
		int maxEdits = Math.max(0, m / 3);
		if (rawDistance > maxEdits)
			return 1.0f;

		// Normalize
		float result = (float) rawDistance / m;

		// Match boosting
		result = target.contains(query) ? result * 0.5f : result;

		return result;
	}

	public void bumpCog(float force) {
		ConfigScreen.cogSpin.bump(3, force);
	}

	public static abstract class Entry extends ObjectSelectionList.Entry<Entry> implements TickableGuiEventListener {
		protected List<GuiEventListener> listeners;
		protected Map<String, String> annotations;
		@Nullable
		protected String path;

		protected Entry() {
			listeners = new ArrayList<>();
			annotations = new HashMap<>();
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			return getGuiListeners().stream().anyMatch(l -> l.mouseClicked(event, doubleClick));
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			return getGuiListeners().stream().anyMatch(l -> l.keyPressed(event));
		}

		@Override
		public boolean charTyped(CharacterEvent event) {
			for (GuiEventListener l : getGuiListeners()) {
				if (l.charTyped(event)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public void tick() {
		}

		public List<GuiEventListener> getGuiListeners() {
			return listeners;
		}

		protected void setEditable(boolean b) {
		}

		protected boolean isCurrentValueChanged() {
			if (path == null) {
				return false;
			}
			return net.createmod.catnip.api.config.ConfigHelper.changes.containsKey(path);
		}
	}

	public static class LabeledEntry extends Entry {
		protected static final float labelWidthMult = 0.4f;

		protected TextStencilElement label;
		protected List<Component> labelTooltip;
		@Nullable
		protected String unit = null;
		protected LerpedFloat differenceAnimation = LerpedFloat.linear().startWithValue(0);
		protected LerpedFloat highlightAnimation = LerpedFloat.linear().startWithValue(0);

		public LabeledEntry(String label) {
			this.label = new TextStencilElement(Minecraft.getInstance().font, label);
			this.label.withElementRenderer((graphics, width, height, alpha) -> UIRenderHelper.angledGradient(graphics, 0, 0, height / 2, height, width, UIRenderHelper.COLOR_TEXT_STRONG_ACCENT));
			labelTooltip = new ArrayList<>();
		}

		public LabeledEntry(String label, String path) {
			this(label);
			this.path = path;
		}

		@Override
		public void tick() {
			differenceAnimation.tickChaser();
			highlightAnimation.tickChaser();
			super.tick();
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
			if (isCurrentValueChanged()) {
				if (differenceAnimation.getChaseTarget() != 1)
					differenceAnimation.chase(1, .5f, Chaser.EXP);
			} else {
				if (differenceAnimation.getChaseTarget() != 0)
					differenceAnimation.chase(0, .6f, Chaser.EXP);
			}

			float animation = differenceAnimation.getValue(partialTick);
			if (animation > .1f) {
				int offset = (int) (30 * (1 - animation));

				if (annotations.containsKey(RequiresRestart.CLIENT.getName())) {
					UIRenderHelper.streak(graphics, 180, getX() + getWidth() + 10 + offset, getY() + getHeight() / 2, getHeight() - 6, 110, new Color(0x50_601010));
				} else if (annotations.containsKey(RequiresRelog.TRUE.getName())) {
					UIRenderHelper.streak(graphics, 180, getX() + getWidth() + 10 + offset, getY() + getHeight() / 2, getHeight() - 6, 110, new Color(0x40_eefb17));
				}

				UIRenderHelper.breadcrumbArrow(graphics, getX() - 10 - offset, getY() + 6, -20, 24, -18, new Color(0x70_ffffff), Color.TRANSPARENT_BLACK);
			}

			UIRenderHelper.streak(graphics, 0, getX() - 10, getY() + getHeight() / 2, getHeight() - 6, getWidth() / 8 * 7, new Color(0xdd_000000));
			UIRenderHelper.streak(graphics, 180, getX() + (int) (getWidth() * 1.35f) + 10, getY() + getHeight() / 2, getHeight() - 6, getWidth() / 8 * 7, new Color(0xdd_000000));
			MutableComponent component = label.getComponent();
			Font font = Minecraft.getInstance().font;
			if (font.width(component) > getLabelWidth(getWidth()) - 10) {
				label.withText(font.substrByWidth(component, getLabelWidth(getWidth()) - 15).getString() + "...");
			}
			if (unit != null) {
				int unitWidth = font.width(unit);
				graphics.text(font, unit, getX() + getLabelWidth(getWidth()) - unitWidth - 5, getY() + getHeight() / 2 + 2, UIRenderHelper.COLOR_TEXT_DARKER.getFirst().getRGB());
				label.at(getX() + 10, getY() + getHeight() / 2f - 10, 0).submit(graphics);
			} else {
				label.at(getX() + 10, getY() + getHeight() / 2f - 4, 0).submit(graphics);
			}

			if (annotations.containsKey("highlight")) {
				highlightAnimation.startWithValue(1).chase(0, 0.1f, Chaser.LINEAR);
				annotations.remove("highlight");
			}

			animation = highlightAnimation.getValue(partialTick);
			if (animation > .01f) {
				Color highlight = new Color(0xa0_ffffff).scaleAlpha(animation);
				UIRenderHelper.streak(graphics, 0, getX() - 10, getY() + getHeight() / 2, getHeight() - 6, 5, highlight);
				UIRenderHelper.streak(graphics, 180, getX() + getWidth(), getY() + getHeight() / 2, getHeight() - 6, 5, highlight);
				UIRenderHelper.streak(graphics, 90, getX() + getWidth() / 2 - 5, getY() + 3, getWidth() + 10, 5, highlight);
				UIRenderHelper.streak(graphics, -90, getX() + getWidth() / 2 - 5, getY() + getHeight() - 3, getWidth() + 10, 5, highlight);
			}


			if (mouseX > getX() && mouseX < getX() + getLabelWidth(getWidth()) && mouseY > getY() + 5 && mouseY < getY() + getHeight() - 5) {
				List<Component> tooltip = getLabelTooltip();
				if (tooltip.isEmpty())
					return;

				// deferred to the end of the frame, outside the list's scissor
				graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
			}
		}


		public List<Component> getLabelTooltip() {
			return labelTooltip;
		}

		protected int getLabelWidth(int totalWidth) {
			return totalWidth;
		}

		@Override
		public Component getNarration() {
			return CommonComponents.EMPTY;
		}
	}
}
