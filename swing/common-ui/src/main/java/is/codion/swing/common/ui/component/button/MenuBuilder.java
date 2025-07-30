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
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuListener;
import java.util.function.Function;

/**
 * A builder for menus.
 */
public interface MenuBuilder extends ComponentBuilder<Void, JMenu, MenuBuilder> {

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
	 * @param actionMenuItem the function to use when creating action based menu items
	 * @return this builder instance
	 */
	MenuBuilder actionMenuItem(Function<Action, JMenuItem> actionMenuItem);

	/**
	 * @param controlMenuItem the function to use when creating control based menu items
	 * @return this builder instance
	 */
	MenuBuilder controlMenuItem(Function<Control, JMenuItem> controlMenuItem);

	/**
	 * @param toggleControlMenuItem the function to use when creating toggle control based menu items
	 * @return this builder instance
	 */
	MenuBuilder toggleControlMenuItem(Function<ToggleControl, JCheckBoxMenuItem> toggleControlMenuItem);

	/**
	 * @return a new JPopupMenu based on this menu builder
	 */
	JPopupMenu buildPopupMenu();

	/**
	 * @return a new JMenuBar based on this menu builder
	 */
	JMenuBar buildMenuBar();

	/**
	 * Provides a {@link MenuBuilder}
	 */
	interface ControlsStep {

		/**
		 * @param action the action to base the menu on
		 * @return a builder instance
		 */
		MenuBuilder action(Action action);

		/**
		 * @param control the control to base the menu on
		 * @return a builder instance
		 */
		MenuBuilder control(Control control);

		/**
		 * @param control the control to base the menu on
		 * @return a builder instance
		 */
		MenuBuilder control(Control.Builder<?, ?> control);

		/**
		 * @param controls the controls to base the menu on
		 * @return a builder instance
		 */
		MenuBuilder controls(Controls controls);

		/**
		 * @param controls the controls to base the menu on
		 * @return a builder instance
		 */
		MenuBuilder controls(ControlsBuilder controls);
	}

	/**
	 * @return a new MenuBuilder
	 */
	static MenuBuilder.ControlsStep builder() {
		return DefaultMenuBuilder.CONTROLS;
	}
}
