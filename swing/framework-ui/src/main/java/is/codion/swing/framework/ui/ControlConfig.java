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
import is.codion.swing.common.ui.control.Controls;

/**
 * Configures controls.
 * @param <T> the type used to identify the standard controls available
 */
public interface ControlConfig<T extends Enum<T>, C extends ControlConfig<T, C>> {

	/**
	 * Adds a separator
	 * @return this config instance
	 */
	C separator();

	/**
	 * Adds a standard control
	 * @return this config instance
	 */
	C standard(T control);

	/**
	 * @param control the control to add
	 * @return this config instance
	 */
	C control(Control control);

	/**
	 * Adds all remaining default controls
	 * @return this config instance
	 */
	C defaults();

	/**
	 * Adds all remaining default controls, stopping before {@code stopBefore}
	 * @param stopBefore the table control to stop before
	 * @return this config instance
	 */
	C defaults(T stopBefore);

	/**
	 * Clears all controls from this config
	 * @return this config instance
	 */
	C clear();

	/**
	 * @return a {@link Controls} instance based on this config
	 */
	Controls createControls();
}
