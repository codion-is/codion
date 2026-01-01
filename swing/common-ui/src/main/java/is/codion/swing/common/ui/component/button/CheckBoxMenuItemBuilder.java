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

import javax.swing.JCheckBoxMenuItem;

/**
 * Builds a JCheckBoxMenuItem.
 */
public interface CheckBoxMenuItemBuilder<B extends CheckBoxMenuItemBuilder<B>> extends ToggleMenuItemBuilder<JCheckBoxMenuItem, B> {

	/**
	 * @param <B> the builder type
	 * @return a builder for a JCheckBoxMenuItem
	 */
	static <B extends CheckBoxMenuItemBuilder<B>> CheckBoxMenuItemBuilder<B> builder() {
		return new DefaultCheckBoxMenuItemBuilder<>();
	}
}
