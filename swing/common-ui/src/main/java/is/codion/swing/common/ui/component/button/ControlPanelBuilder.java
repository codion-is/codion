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

import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builds panels with controls.
 */
public interface ControlPanelBuilder<C extends JComponent, B extends ControlPanelBuilder<C, B>>
				extends ComponentBuilder<C, B> {

	/**
	 * @param orientation the panel orientation, default {@link javax.swing.SwingConstants#HORIZONTAL}
	 * @return this builder instance
	 */
	B orientation(int orientation);

	/**
	 * @param includeButtonText true if buttons should include text
	 * @return this builder instance
	 */
	B includeButtonText(boolean includeButtonText);

	/**
	 * @param preferredButtonSize the preferred button size
	 * @return this builder instance
	 */
	B preferredButtonSize(@Nullable Dimension preferredButtonSize);

	/**
	 * @param buttonsFocusable whether the buttons should be focusable, default is {@code true}
	 * @return this builder instance
	 */
	B buttonsFocusable(boolean buttonsFocusable);

	/**
	 * Specifies how toggle controls are presented on this control panel.
	 * The default is {@link ToggleButtonType#BUTTON}.
	 * @param toggleButtonType the toggle button type
	 * @return this builder instance
	 */
	B toggleButtonType(ToggleButtonType toggleButtonType);

	/**
	 * Provides a way to configure the {@link ButtonBuilder} used by this {@link ControlPanelBuilder}.
	 * @param builder provides the button builder used to create buttons
	 * @return this builder instance
	 */
	B button(Consumer<ButtonBuilder<?, ?, ?>> builder);

	/**
	 * Provides a way to configure the {@link ToggleButtonBuilder} used by this {@link ControlPanelBuilder}.
	 * @param builder provides the toggle button builder used to create toggle buttons
	 * @return this builder instance
	 */
	B toggleButton(Consumer<ToggleButtonBuilder<?, ?>> builder);

	/**
	 * Provides a way to configure the {@link CheckBoxBuilder} used by this {@link ControlPanelBuilder}.
	 * @param builder provides the toggle button builder used to create check boxes
	 * @return this builder instance
	 */
	B checkBox(Consumer<CheckBoxBuilder> builder);

	/**
	 * Provides a way to configure the {@link RadioButtonBuilder} used by this {@link ControlPanelBuilder}.
	 * @param builder provides the toggle button builder used to create radio buttons
	 * @return this builder instance
	 */
	B radioButton(Consumer<RadioButtonBuilder> builder);

	/**
	 * Provides a {@link ControlPanelBuilder}
	 * @param <C> the component type
	 * @param <B> the builder type
	 */
	interface ControlsStep<C extends JComponent, B extends ControlPanelBuilder<C, B>> {

		/**
		 * @param action the action to base the panel on
		 * @return this builder instance
		 */
		B action(Action action);

		/**
		 * @param control the control to base the panel on
		 * @return this builder instance
		 */
		B control(Control control);

		/**
		 * @param control the control to base the panel on
		 * @return this builder instance
		 */
		B control(Supplier<? extends Control> control);

		/**
		 * @param controls the controls to base the panel on
		 * @return this builder instance
		 */
		B controls(Controls controls);

		/**
		 * @param controls the controls to base the panel on
		 * @return this builder instance
		 */
		B controls(Supplier<Controls> controls);
	}
}
