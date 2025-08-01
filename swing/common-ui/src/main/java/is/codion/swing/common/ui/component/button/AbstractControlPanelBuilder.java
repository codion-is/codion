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
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

abstract class AbstractControlPanelBuilder<C extends JComponent, B extends ControlPanelBuilder<C, B>>
				extends AbstractComponentBuilder<Void, C, B> implements ControlPanelBuilder<C, B> {

	private final ButtonBuilder<?, ?, ?> buttonBuilder = ButtonBuilder.builder();
	private final ToggleButtonBuilder<?, ?> toggleButtonBuilder = ToggleButtonBuilder.builder();
	private final CheckBoxBuilder checkBoxBuilder = CheckBoxBuilder.builder();
	private final RadioButtonBuilder radioButtonBuilder = RadioButtonBuilder.builder();

	private int orientation = SwingConstants.HORIZONTAL;
	private ToggleButtonType toggleButtonType = ToggleButtonType.BUTTON;

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
		buttonBuilder.includeText(includeButtonText);
		toggleButtonBuilder.includeText(includeButtonText);
		checkBoxBuilder.includeText(includeButtonText);
		radioButtonBuilder.includeText(includeButtonText);
		return self();
	}

	@Override
	public final B preferredButtonSize(@Nullable Dimension preferredButtonSize) {
		buttonBuilder.preferredSize(preferredButtonSize);
		toggleButtonBuilder.preferredSize(preferredButtonSize);
		checkBoxBuilder.preferredSize(preferredButtonSize);
		radioButtonBuilder.preferredSize(preferredButtonSize);
		return self();
	}

	@Override
	public final B buttonsFocusable(boolean buttonsFocusable) {
		buttonBuilder.focusable(buttonsFocusable);
		toggleButtonBuilder.focusable(buttonsFocusable);
		checkBoxBuilder.focusable(buttonsFocusable);
		radioButtonBuilder.focusable(buttonsFocusable);
		return self();
	}

	@Override
	public final B toggleButtonType(ToggleButtonType toggleButtonType) {
		this.toggleButtonType = requireNonNull(toggleButtonType);
		return self();
	}

	@Override
	public final B button(Consumer<ButtonBuilder<?, ?, ?>> builder) {
		requireNonNull(builder).accept(this.buttonBuilder);
		return self();
	}

	@Override
	public final B toggleButton(Consumer<ToggleButtonBuilder<?, ?>> builder) {
		requireNonNull(builder).accept(this.toggleButtonBuilder);
		return self();
	}

	@Override
	public final B checkBox(Consumer<CheckBoxBuilder> builder) {
		requireNonNull(builder).accept(this.checkBoxBuilder);
		return self();
	}

	@Override
	public final B radioButton(Consumer<RadioButtonBuilder> builder) {
		requireNonNull(builder).accept(this.radioButtonBuilder);
		return self();
	}

	@Override
	protected final ComponentValue<Void, C> createComponentValue(C component) {
		return new PanelComponentValue<>(component);
	}

	protected final int orientation() {
		return orientation;
	}

	protected final ButtonBuilder<?, ?, ?> buttonBuilder() {
		return buttonBuilder;
	}

	protected final ToggleButtonBuilder<?, ?> toggleButtonBuilder() {
		switch (toggleButtonType) {
			case CHECKBOX:
				return checkBoxBuilder;
			case BUTTON:
				return toggleButtonBuilder;
			case RADIO_BUTTON:
				return radioButtonBuilder;
			default:
				throw new IllegalArgumentException("Unknown toggle button type: " + toggleButtonType);
		}
	}

	private static final class PanelComponentValue<C extends JComponent> extends AbstractComponentValue<Void, C> {

		private PanelComponentValue(C component) {
			super(component);
		}

		@Override
		protected Void getComponentValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void setComponentValue(Void value) {
			throw new UnsupportedOperationException();
		}
	}
}
