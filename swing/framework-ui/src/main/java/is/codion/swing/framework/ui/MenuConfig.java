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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.swing.common.ui.control.Control;

/**
 * Configures a menu.
 * @param <T> the type used to identify standard controls
 */
public interface MenuConfig<T extends Enum<T>> {

	/**
	 * Adds a separator
	 * @return this config instance
	 */
	MenuConfig<T> separator();

	/**
	 * Adds a standard control
	 * @return this config instance
	 */
	MenuConfig<T> standard(T control);

	/**
	 * @param control the control to add
	 * @return this config instance
	 */
	MenuConfig<T> control(Control control);

	/**
	 * Adds all remaining default controls
	 * @return this config instance
	 */
	MenuConfig<T> defaults();

	/**
	 * Adds all remaining default controls, stopping at {@code stop}
	 * @param stop the table control to stop at
	 * @return this config instance
	 */
	MenuConfig<T> defaults(T stop);

	/**
	 * Clears all controls from this config
	 * @return this config instance
	 */
	MenuConfig<T> clear();
}
