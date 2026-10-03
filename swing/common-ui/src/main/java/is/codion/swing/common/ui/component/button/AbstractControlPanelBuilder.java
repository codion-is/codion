/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

abstract class AbstractControlPanelBuilder<C extends JComponent, B extends ControlPanelBuilder<C, B>>
				extends AbstractComponentBuilder<C, B> implements ControlPanelBuilder<C, B> {

	private static final EmptyConsumer<?> EMPTY_CONSUMER = new EmptyConsumer<>();

	private final List<Item> items = new ArrayList<>();

	private Consumer<ButtonBuilder<?, ?, ?>> buttons = (Consumer<ButtonBuilder<?, ?, ?>>) EMPTY_CONSUMER;
	private Consumer<ToggleButtonBuilder<?, ?>> toggleButtons = (Consumer<ToggleButtonBuilder<?, ?>>) EMPTY_CONSUMER;
	private Consumer<CheckBoxBuilder> checkBoxes = (Consumer<CheckBoxBuilder>) EMPTY_CONSUMER;
	private Consumer<RadioButtonBuilder> radioButtons = (Consumer<RadioButtonBuilder>) EMPTY_CONSUMER;

	private int orientation = SwingConstants.HORIZONTAL;
	private ToggleButtonType toggleButtonType = ToggleButtonType.BUTTON;
	private boolean includeButtonText = true;
	private @Nullable Dimension preferredButtonSize;
	private boolean buttonsFocusable = true;

	protected AbstractControlPanelBuilder() {}

	@Override
	public final B action(Action action) {
		requireNonNull(action);
		// empty controls are left out, as by Controls, keeping the separator cleanup accurate
		if (!(action instanceof Controls) || ((Controls) action).size() > 0) {
			items.add(new ActionItem(action));
		}
		return self();
	}

	@Override
	public final B control(Control control) {
		return action(control);
	}

	@Override
	public final B control(Supplier<? extends Control> control) {
		return action(requireNonNull(control).get());
	}

	@Override
	public final B controls(Controls controls) {
		requireNonNull(controls).actions().forEach(this::action);
		return self();
	}

	@Override
	public final B controls(Supplier<Controls> controls) {
		return controls(requireNonNull(controls).get());
	}

	@Override
	public final B separator() {
		return action(Controls.SEPARATOR);
	}

	@Override
	public final B add(JComponent component) {
		requireNonNull(component);
		items.add(new ComponentItem(() -> component));
		return self();
	}

	@Override
	public final B add(Supplier<? extends JComponent> component) {
		items.add(new ComponentItem(requireNonNull(component)));
		return self();
	}

	@Override
	public final B orientation(int orientation) {
		if (orientation != SwingConstants.VERTICAL && orientation != SwingConstants.HORIZONTAL) {
			throw new IllegalArgumentException("Unknown orientation value: " + orientation);
		}
		this.orientation = orientation;
		return self();
	}

	@Override
	public final B includeButtonText(boolean includeButtonText) {
		this.includeButtonText = includeButtonText;
		return self();
	}

	@Override
	public final B preferredButtonSize(@Nullable Dimension preferredButtonSize) {
		this.preferredButtonSize = preferredButtonSize;
		return self();
	}

	@Override
	public final B buttonsFocusable(boolean buttonsFocusable) {
		this.buttonsFocusable = buttonsFocusable;
		return self();
	}

	@Override
	public final B toggleButtonType(ToggleButtonType toggleButtonType) {
		this.toggleButtonType = requireNonNull(toggleButtonType);
		return self();
	}

	@Override
	public final B buttons(Consumer<ButtonBuilder<?, ?, ?>> buttons) {
		this.buttons = requireNonNull(buttons);
		return self();
	}

	@Override
	public final B toggleButtons(Consumer<ToggleButtonBuilder<?, ?>> toggleButtons) {
		this.toggleButtons = requireNonNull(toggleButtons);
		return self();
	}

	@Override
	public final B checkBoxes(Consumer<CheckBoxBuilder> checkBoxes) {
		this.checkBoxes = requireNonNull(checkBoxes);
		return self();
	}

	@Override
	public final B radioButtons(Consumer<RadioButtonBuilder> radioButtons) {
		this.radioButtons = requireNonNull(radioButtons);
		return self();
	}

	protected final int orientation() {
		return orientation;
	}

	/**
	 * Adds the content in the order added, leading, trailing and adjacent duplicate separators removed.
	 * @param handler handles the actions
	 * @param components receives the components
	 */
	protected final void addContent(ControlHandler handler, Consumer<JComponent> components) {
		ControlHandler.cleanupSeparators(items, Item::separator)
						.forEach(item -> item.add(handler, components));
	}

	protected final ButtonBuilder<?, ?, ?> buttonBuilder() {
		ButtonBuilder<JButton, Void, ?> buttonBuilder = set(ButtonBuilder.builder());
		buttons.accept(buttonBuilder);

		return buttonBuilder;
	}

	protected final ToggleButtonBuilder<?, ?> toggleButtonBuilder() {
		switch (toggleButtonType) {
			case CHECKBOX:
				CheckBoxBuilder checkBoxBuilder = set(CheckBoxBuilder.builder());
				checkBoxes.accept(checkBoxBuilder);

				return checkBoxBuilder;
			case BUTTON:
				ToggleButtonBuilder<?, ?> toggleButtonBuilder = set(ToggleButtonBuilder.builder());
				toggleButtons.accept(toggleButtonBuilder);

				return toggleButtonBuilder;
			case RADIO_BUTTON:
				RadioButtonBuilder radioButtonBuilder = set(RadioButtonBuilder.builder());
				radioButtons.accept(radioButtonBuilder);

				return radioButtonBuilder;
			default:
				throw new IllegalArgumentException("Unknown toggle button type: " + toggleButtonType);
		}
	}

	private <T extends ButtonBuilder<?, ?, ?>> T set(T builder) {
		return (T) builder.includeText(includeButtonText)
						.preferredSize(preferredButtonSize)
						.focusable(buttonsFocusable);
	}

	private static final class EmptyConsumer<T> implements Consumer<T> {

		@Override
		public void accept(T result) {}
	}

	private interface Item {

		boolean separator();

		void add(ControlHandler handler, Consumer<JComponent> components);
	}

	private static final class ActionItem implements Item {

		private final Action action;

		private ActionItem(Action action) {
			this.action = action;
		}

		@Override
		public boolean separator() {
			return action == Controls.SEPARATOR;
		}

		@Override
		public void add(ControlHandler handler, Consumer<JComponent> components) {
			handler.accept(action);
		}
	}

	private static final class ComponentItem implements Item {

		private final Supplier<? extends JComponent> component;

		private ComponentItem(Supplier<? extends JComponent> component) {
			this.component = component;
		}

		@Override
		public boolean separator() {
			return false;
		}

		@Override
		public void add(ControlHandler handler, Consumer<JComponent> components) {
			components.accept(requireNonNull(component.get()));
		}
	}
}
