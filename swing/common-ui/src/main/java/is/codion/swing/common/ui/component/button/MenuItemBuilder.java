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

import org.jspecify.annotations.Nullable;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/**
 * Builds a JMenuItem.
 */
public interface MenuItemBuilder<B extends MenuItemBuilder<B>> extends ButtonBuilder<JMenuItem, Void, B> {

	/**
	 * Sets the accelerator displayed by the menu item, in place of any accelerator of its action.
	 * <p>Note that Swing binds the accelerator as well, in the window of a menu bar containing the menu item,
	 * and while a popup menu containing it is showing.
	 * @param accelerator the accelerator, null for the one of the action, if any
	 * @return this builder instance
	 * @see JMenuItem#setAccelerator(KeyStroke)
	 */
	B accelerator(@Nullable KeyStroke accelerator);

	/**
	 * @param <B> the builder type
	 * @return a builder for a JMenuItem
	 */
	static <B extends MenuItemBuilder<B>> MenuItemBuilder<B> builder() {
		return new DefaultMenuItemBuilder<>();
	}
}
