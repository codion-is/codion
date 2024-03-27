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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

abstract class AbstractControlPanelBuilder<C extends JComponent, B extends ControlPanelBuilder<C, B>>
				extends AbstractComponentBuilder<Void, C, B> implements ControlPanelBuilder<C, B> {

	private final Controls controls = Controls.controls();

	private final ButtonBuilder<?, ?, ?> buttonBuilder = ButtonBuilder.builder();
	private final ToggleButtonBuilder<?, ?> toggleButtonBuilder = ToggleButtonBuilder.builder();
	private final CheckBoxBuilder checkBoxBuilder = CheckBoxBuilder.builder();
	private final RadioButtonBuilder radioButtonBuilder = RadioButtonBuilder.builder();

	private int orientation = SwingConstants.HORIZONTAL;
	private ToggleButtonType toggleButtonType = ToggleButtonType.BUTTON;

	protected AbstractControlPanelBuilder(Controls controls) {
		if (controls != null) {
			this.controls.addAll(controls);
		}
	}

	@Override
	public final B orientation(int orientation) {
		if (orientation != SwingConstants.VERTICAL && orientation != SwingConstants.HORIZONTAL) {
			throw new IllegalArgumentException("Unknown orientation value: " + orientation);
		}
		this.orientation = orientation;
		return (B) this;
	}

	@Override
	public final B action(Action action) {
		this.controls.add(requireNonNull(action));
		return (B) this;
	}

	@Override
	public final B controls(Controls controls) {
		this.controls.addAll(requireNonNull(controls));
		return (B) this;
	}

	@Override
	public final B separator() {
		this.controls.addSeparator();
		return (B) this;
	}

	@Override
	public final B includeButtonText(boolean includeButtonText) {
		buttonBuilder.includeText(includeButtonText);
		toggleButtonBuilder.includeText(includeButtonText);
		checkBoxBuilder.includeText(includeButtonText);
		radioButtonBuilder.includeText(includeButtonText);
		return (B) this;
	}

	@Override
	public final B preferredButtonSize(Dimension preferredButtonSize) {
		buttonBuilder.preferredSize(preferredButtonSize);
		toggleButtonBuilder.preferredSize(preferredButtonSize);
		checkBoxBuilder.preferredSize(preferredButtonSize);
		radioButtonBuilder.preferredSize(preferredButtonSize);
		return (B) this;
	}

	@Override
	public final B buttonsFocusable(boolean buttonsFocusable) {
		buttonBuilder.focusable(buttonsFocusable);
		toggleButtonBuilder.focusable(buttonsFocusable);
		checkBoxBuilder.focusable(buttonsFocusable);
		radioButtonBuilder.focusable(buttonsFocusable);
		return (B) this;
	}

	@Override
	public final B toggleButtonType(ToggleButtonType toggleButtonType) {
		this.toggleButtonType = requireNonNull(toggleButtonType);
		return (B) this;
	}

	@Override
	public final B buttonBuilder(Consumer<ButtonBuilder<?, ?, ?>> buttonBuilder) {
		requireNonNull(buttonBuilder).accept(this.buttonBuilder);
		return (B) this;
	}

	@Override
	public final B toggleButtonBuilder(Consumer<ToggleButtonBuilder<?, ?>> toggleButtonBuilder) {
		requireNonNull(toggleButtonBuilder).accept(this.toggleButtonBuilder);
		return (B) this;
	}

	@Override
	public B checkBoxBuilder(Consumer<CheckBoxBuilder> checkBoxBuilder) {
		requireNonNull(checkBoxBuilder).accept(this.checkBoxBuilder);
		return (B) this;
	}

	@Override
	public B radioButtonBuilder(Consumer<RadioButtonBuilder> radioButtonBuilder) {
		requireNonNull(radioButtonBuilder).accept(this.radioButtonBuilder);
		return (B) this;
	}

	@Override
	protected final ComponentValue<Void, C> createComponentValue(C component) {
		throw new UnsupportedOperationException("A ComponentValue can not be based on this component type");
	}

	@Override
	protected final void setInitialValue(C component, Void initialValue) {}

	protected final Controls controls() {
		return controls;
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
}
