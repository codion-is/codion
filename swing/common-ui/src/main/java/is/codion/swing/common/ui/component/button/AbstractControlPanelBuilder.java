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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

abstract class AbstractControlPanelBuilder<C extends JComponent, B extends ControlPanelBuilder<C, B>>
				extends AbstractComponentBuilder<C, B> implements ControlPanelBuilder<C, B> {

	private static final EmptyConsumer<?> EMPTY_CONSUMER = new EmptyConsumer<>();

	private Consumer<ButtonBuilder<?, ?, ?>> button = (Consumer<ButtonBuilder<?, ?, ?>>) EMPTY_CONSUMER;
	private Consumer<ToggleButtonBuilder<?, ?>> toggleButton = (Consumer<ToggleButtonBuilder<?, ?>>) EMPTY_CONSUMER;
	private Consumer<CheckBoxBuilder> checkBox = (Consumer<CheckBoxBuilder>) EMPTY_CONSUMER;
	private Consumer<RadioButtonBuilder> radioButton = (Consumer<RadioButtonBuilder>) EMPTY_CONSUMER;

	private int orientation = SwingConstants.HORIZONTAL;
	private ToggleButtonType toggleButtonType = ToggleButtonType.BUTTON;
	private boolean includeButtonText = true;
	private @Nullable Dimension preferredButtonSize;
	private boolean buttonsFocusable = true;

	protected AbstractControlPanelBuilder() {}

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
	public final B button(Consumer<ButtonBuilder<?, ?, ?>> builder) {
		this.button = requireNonNull(builder);
		return self();
	}

	@Override
	public final B toggleButton(Consumer<ToggleButtonBuilder<?, ?>> builder) {
		this.toggleButton = requireNonNull(builder);
		return self();
	}

	@Override
	public final B checkBox(Consumer<CheckBoxBuilder> builder) {
		this.checkBox = requireNonNull(builder);
		return self();
	}

	@Override
	public final B radioButton(Consumer<RadioButtonBuilder> builder) {
		this.radioButton = requireNonNull(builder);
		return self();
	}

	protected final int orientation() {
		return orientation;
	}

	protected final ButtonBuilder<?, ?, ?> buttonBuilder() {
		ButtonBuilder<JButton, Void, ?> buttonBuilder = set(ButtonBuilder.builder());
		button.accept(buttonBuilder);

		return buttonBuilder;
	}

	protected final ToggleButtonBuilder<?, ?> toggleButtonBuilder() {
		switch (toggleButtonType) {
			case CHECKBOX:
				CheckBoxBuilder checkBoxBuilder = set(CheckBoxBuilder.builder());
				checkBox.accept(checkBoxBuilder);

				return checkBoxBuilder;
			case BUTTON:
				ToggleButtonBuilder<?, ?> toggleButtonBuilder = set(ToggleButtonBuilder.builder());
				toggleButton.accept(toggleButtonBuilder);

				return toggleButtonBuilder;
			case RADIO_BUTTON:
				RadioButtonBuilder radioButtonBuilder = set(RadioButtonBuilder.builder());
				radioButton.accept(radioButtonBuilder);

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
}
