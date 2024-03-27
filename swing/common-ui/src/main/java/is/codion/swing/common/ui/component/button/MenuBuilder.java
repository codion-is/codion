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

import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuListener;

import static java.util.Objects.requireNonNull;

/**
 * A builder for menus.
 */
public interface MenuBuilder extends ComponentBuilder<Void, JMenu, MenuBuilder>, MenuItemBuilder<JMenu, MenuBuilder> {

	/**
	 * Adds all actions from the given {@link Controls} instance
	 * @param controls the Controls instance
	 * @return this builder instance
	 */
	MenuBuilder controls(Controls controls);

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
	 * @see #createPopupMenu()
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
	JPopupMenu createPopupMenu();

	/**
	 * @return a new JMenuBar based on this menu builder
	 */
	JMenuBar createMenuBar();

	/**
	 * @return a new MenuBuilder
	 */
	static MenuBuilder builder() {
		return new DefaultMenuBuilder(null);
	}

	/**
	 * @param controls the controls to base the menu on
	 * @return a new MenuBuilder based on the given controls
	 */
	static MenuBuilder builder(Controls controls) {
		return new DefaultMenuBuilder(requireNonNull(controls));
	}

	/**
	 * @param controlsBuilder the controls builder to base the menu on
	 * @return a new MenuBuilder based on the given controls
	 */
	static MenuBuilder builder(Controls.Builder controlsBuilder) {
		return new DefaultMenuBuilder(requireNonNull(controlsBuilder).build());
	}
}
