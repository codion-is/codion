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

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.swing.common.ui.control.ToggleControl;

import org.jspecify.annotations.Nullable;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.util.function.Supplier;

import static is.codion.common.utilities.Configuration.enumValue;

/**
 * Builds a toggle menu item.
 */
public interface ToggleMenuItemBuilder<C extends JMenuItem, B extends ToggleMenuItemBuilder<C, B>> extends ButtonBuilder<C, Boolean, B> {

	/**
	 * Specifies whether a menu is kept open after a toggle menu item has been toggled.
	 */
	enum PersistMenu {
		/**
		 * Always keep the menu open.
		 */
		ALWAYS,
		/**
		 * Keep the menu open if CTRL is down when clicked
		 */
		CTRL_DOWN,
		/**
		 * Always close the menu when clicked.
		 */
		NEVER
	}

	/**
	 * Specifies whether a menu persists after a toggle menu item has been toggled
	 * <ul>
	 * <li>Value type: {@link PersistMenu}
	 * <li>Default value: {@link PersistMenu#ALWAYS}
	 * </ul>
	 */
	PropertyValue<PersistMenu> PERSIST_MENU =
					enumValue(ToggleMenuItemBuilder.class.getName() + ".persistMenu",
									PersistMenu.class, PersistMenu.ALWAYS);

	/**
	 * Links the value of the given toggle control to the menu item, the menu item toggling it, the control providing
	 * the caption, icon and enabled state.
	 * @param toggleControl the toggle control to base this toggle menu item on
	 * @return this builder instance
	 * @throws IllegalArgumentException in case the value is nullable
	 */
	B toggle(ToggleControl toggleControl);

	/**
	 * @param toggleControl the toggle control to base this toggle menu on
	 * @return this builder instance
	 */
	B toggle(Supplier<ToggleControl> toggleControl);

	/**
	 * Creates a bidirectional link to the given state. Overrides any initial value set.
	 * @param linkedState a state to link to the component value
	 * @return this builder instance
	 */
	B link(State linkedState);

	/**
	 * Creates a read-only link to the given {@link ObservableState}.
	 * @param linkedState a state to link to the component value
	 * @return this builder instance
	 */
	B link(ObservableState linkedState);

	/**
	 * @param persistMenu specifies when a menu persists after a toggle button click
	 * @return this builder instance
	 */
	B persistMenu(PersistMenu persistMenu);

	/**
	 * Sets the accelerator displayed by the menu item, in place of any accelerator of its action.
	 * <p>Note that Swing binds the accelerator as well, in the window of a menu bar containing the menu item,
	 * and while a popup menu containing it is showing.
	 * @param accelerator the accelerator, null for the one of the action, if any
	 * @return this builder instance
	 * @see JMenuItem#setAccelerator(KeyStroke)
	 */
	B accelerator(@Nullable KeyStroke accelerator);
}
