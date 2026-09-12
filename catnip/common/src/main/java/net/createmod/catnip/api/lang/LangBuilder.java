package net.createmod.catnip.api.lang;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.data.codec.CatnipCodecUtils;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class LangBuilder {
	String namespace;
	@Nullable
	MutableComponent component;

	public LangBuilder(String namespace) {
		this.namespace = namespace;
	}

	public LangBuilder space() {
		return text(" ");
	}

	public LangBuilder newLine() {
		return text("\n");
	}

	/**
	 * Appends a localised component<br>
	 * To add an independently formatted localised component, use add() and a nested
	 * builder
	 *
	 * @param langKey
	 * @param args
	 * @return
	 */
	public LangBuilder translate(String langKey, Object... args) {
		Object[] args1 = resolveBuilders(args);
		return add(Component.translatable(namespace + "." + langKey, args1));
	}

	/**
	 * Appends a text component
	 *
	 * @param literalText
	 * @return
	 */
	public LangBuilder text(String literalText) {
		return add(Component.literal(literalText));
	}

	/**
	 * Appends a colored text component
	 *
	 * @param format
	 * @param literalText
	 * @return
	 */
	public LangBuilder text(ChatFormatting format, String literalText) {
		return add(Component.literal(literalText).withStyle(format));
	}

	/**
	 * Appends a colored text component
	 *
	 * @param color
	 * @param literalText
	 * @return
	 */
	public LangBuilder text(int color, String literalText) {
		return add(Component.literal(literalText).withStyle(s -> s.withColor(color)));
	}

	/**
	 * Appends the contents of another builder
	 *
	 * @param otherBuilder
	 * @return
	 */
	public LangBuilder add(LangBuilder otherBuilder) {
		return add(otherBuilder.component());
	}

	/**
	 * Appends a component
	 *
	 * <p>Copied when it is the first thing added, because everything after it appends to whatever is
	 * held here - and appending to a component the caller handed over would edit theirs. On 1.21.1
	 * nothing noticed: an item's name was built fresh for each call. 26.2 returns the {@code ITEM_NAME}
	 * data component itself, which every stack of that item shares, so
	 * {@code builder().add(stack.getHoverName()).text(" x33")} wrote " x33" into the name of that item
	 * everywhere it is ever shown, once per render, and styling the builder recoloured it for good.
	 *
	 * @param customComponent
	 * @return
	 */
	public LangBuilder add(MutableComponent customComponent) {
		component = component == null ? customComponent.copy() : component.append(customComponent);
		return this;
	}

	/**
	 * Appends a component
	 *
	 * @param component the component to append
	 * @return this builder
	 */
	public LangBuilder add(Component component) {
		if (component instanceof MutableComponent mutableComponent)
			return add(mutableComponent);
		else
			return add(component.copy());
	}

	//

	/**
	 * Applies the format to all added components
	 *
	 * @param format
	 * @return
	 */
	public LangBuilder style(ChatFormatting format) {
		assertComponent();
		component = component.withStyle(format);
		return this;
	}

	/**
	 * Applies the color to all added components
	 */
	public LangBuilder color(int color) {
		assertComponent();
		component = component.withStyle(s -> s.withColor(color));
		return this;
	}

	/**
	 * Applies the color to all added components
	 */
	public LangBuilder color(Color color) {
		return this.color(color.getRGB());
	}

	//

	public MutableComponent component() {
		assertComponent();
		return component;
	}

	public String string() {
		return component().getString();
	}

	public String json() {
		return CatnipCodecUtils.encode(ComponentSerialization.CODEC, component()).map(Tag::toString).orElse("");
	}

	public void sendStatus(Player player) {
		player.sendOverlayMessage(component());
	}

	public void sendChat(Player player) {
		player.sendSystemMessage(component());
	}

	public void addTo(List<? super MutableComponent> tooltip) {
		tooltip.add(component());
	}

	public void addTo(Consumer<Component> tooltip) {
		tooltip.accept(component());
	}

	/**
	 * Add this line to a goggle overlay tooltip, indented to sit under its heading.
	 */
	public void forGoggles(List<? super MutableComponent> tooltip) {
		forGoggles(tooltip, 0);
	}

	/**
	 * Note that the indent is a fixed number of spaces. Create used to scale it by the font's space
	 * width so that non-default resource packs lined up, but this class is in the server-safe module
	 * and cannot reach the font.
	 */
	public void forGoggles(List<? super MutableComponent> tooltip, int indents) {
		tooltip.add(new LangBuilder(namespace).text(" ".repeat(4 + indents))
			.add(this)
			.component());
	}

	//

	private void assertComponent() {
		if (component == null)
			throw new IllegalStateException("No components were added to builder");
	}

	//

	public static Object[] resolveBuilders(Object[] args) {
		for (int i = 0; i < args.length; i++)
			if (args[i] instanceof LangBuilder cb)
				args[i] = cb.component();
		return args;
	}
}
