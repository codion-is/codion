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
 * Builds panels with buttons based on controls, along with any other components, in the order added.
 * {@snippet class = is.codion.manual.javadoc.ControlPanelBuilderSnippets region = toolBar :
 * JToolBar toolBar = Components.toolBar()
 * 				.controls(navigationControls)
 * 				.separator()
 * 				.add(searchField)
 * 				// the settings button aligned to the right
 * 				.glue()
 * 				.control(settingsControl)
 * 				.floatable(false)
 * 				.build();}
 * <p>The button configuration, such as {@link #includeButtonText(boolean)} and {@link #buttons(Consumer)}, applies
 * to the buttons based on the controls, components added via {@link #add(JComponent)} are added as is.
 * <p>Leading, trailing and adjacent duplicate separators are removed.
 * @param <C> the component type
 * @param <B> the builder type
 */
public interface ControlPanelBuilder<C extends JComponent, B extends ControlPanelBuilder<C, B>>
				extends ComponentBuilder<C, B> {

	/**
	 * Adds a button based on the given action, or a toggle button in case of a
	 * {@link is.codion.swing.common.ui.control.ToggleControl}.
	 * @param action the action to add
	 * @return this builder instance
	 */
	B action(Action action);

	/**
	 * Adds a button based on the given control, or a toggle button in case of a
	 * {@link is.codion.swing.common.ui.control.ToggleControl}.
	 * @param control the control to add
	 * @return this builder instance
	 */
	B control(Control control);

	/**
	 * Adds a button based on the given control, or a toggle button in case of a
	 * {@link is.codion.swing.common.ui.control.ToggleControl}.
	 * @param control the control to add
	 * @return this builder instance
	 */
	B control(Supplier<? extends Control> control);

	/**
	 * Adds the actions of the given controls.
	 * @param controls the controls to add
	 * @return this builder instance
	 */
	B controls(Controls controls);

	/**
	 * Adds the actions of the given controls.
	 * @param controls the controls to add
	 * @return this builder instance
	 */
	B controls(Supplier<Controls> controls);

	/**
	 * Adds a separator.
	 * @return this builder instance
	 */
	B separator();

	/**
	 * Adds the given component, as is.
	 * @param component the component to add
	 * @return this builder instance
	 */
	B add(JComponent component);

	/**
	 * Adds the component provided by the given supplier when the panel is built, as is.
	 * @param component supplies the component to add
	 * @return this builder instance
	 */
	B add(Supplier<? extends JComponent> component);

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
	 * Configures each {@link ButtonBuilder} used to build the buttons.
	 * @param buttons receives the builder of each button
	 * @return this builder instance
	 */
	B buttons(Consumer<ButtonBuilder<?, ?, ?>> buttons);

	/**
	 * Configures each {@link ToggleButtonBuilder} used to build the toggle buttons, when toggle controls
	 * are presented as toggle buttons, see {@link #toggleButtonType(ToggleButtonType)}.
	 * @param toggleButtons receives the builder of each toggle button
	 * @return this builder instance
	 */
	B toggleButtons(Consumer<ToggleButtonBuilder<?, ?>> toggleButtons);

	/**
	 * Configures each {@link CheckBoxBuilder} used to build the check boxes, when toggle controls
	 * are presented as check boxes, see {@link #toggleButtonType(ToggleButtonType)}.
	 * @param checkBoxes receives the builder of each check box
	 * @return this builder instance
	 */
	B checkBoxes(Consumer<CheckBoxBuilder> checkBoxes);

	/**
	 * Configures each {@link RadioButtonBuilder} used to build the radio buttons, when toggle controls
	 * are presented as radio buttons, see {@link #toggleButtonType(ToggleButtonType)}.
	 * @param radioButtons receives the builder of each radio button
	 * @return this builder instance
	 */
	B radioButtons(Consumer<RadioButtonBuilder> radioButtons);
}
