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
import is.codion.swing.common.ui.control.Controls.ControlsBuilder;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuListener;

/**
 * A builder for menus.
 */
public interface MenuBuilder extends ComponentBuilder<Void, JMenu, MenuBuilder> {

	/**
	 * @param action the action to add
	 * @return this builder instance
	 */
	MenuBuilder action(Action action);

	/**
	 * @param control the control to add
	 * @return this builder instance
	 */
	MenuBuilder control(Control control);

	/**
	 * @param controls the Controls instance on which to base the menu
	 * @return this builder instance
	 */
	MenuBuilder controls(Controls controls);

	/**
	 * @param controls the ControlsBuilder on which to base the menu
	 * @return this builder instance
	 */
	MenuBuilder controls(ControlsBuilder controls);

	/**
	 * Adds a separator
	 * @return this builder instance
	 * @see JMenu#addSeparator()
	 */
	MenuBuilder separator();

	/**
	 * @param menuListener the menu listener
	 * @return this builder instance
	 * @see JMenu#addMenuListener(MenuListener)
	 */
	MenuBuilder menuListener(MenuListener menuListener);

	/**
	 * Has no effect if a popup menu is not created.
	 * @param popupMenuListener the popup menu listener
	 * @return this builder instance
	 * @see #buildPopupMenu()
	 * @see JPopupMenu#addPopupMenuListener(PopupMenuListener)
	 */
	MenuBuilder popupMenuListener(PopupMenuListener popupMenuListener);

	/**
	 * @param menuItemBuilder the menu item builder to use when creating menu items
	 * @return this builder instance
	 */
	MenuBuilder menuItemBuilder(MenuItemBuilder<?, ?> menuItemBuilder);

	/**
	 * @param toggleMenuItemBuilder the toggle menu item builder to use when creating toggle menu items
	 * @return this builder instance
	 */
	MenuBuilder toggleMenuItemBuilder(ToggleMenuItemBuilder<?, ?> toggleMenuItemBuilder);

	/**
	 * @return a new JPopupMenu based on this menu builder
	 */
	JPopupMenu buildPopupMenu();

	/**
	 * @return a new JMenuBar based on this menu builder
	 */
	JMenuBar buildMenuBar();

	/**
	 * @return a new MenuBuilder
	 */
	static MenuBuilder builder() {
		return new DefaultMenuBuilder(null);
	}
}
