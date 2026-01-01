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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import javax.swing.ImageIcon;

/**
 * <p>Provides small and large versions of an icon.
 * <p>For instances use the {@link #controlIcon(ImageIcon, ImageIcon)} factory
 * method or {@link #controlIcon(ImageIcon)} in case of a single icon size.
 */
public interface ControlIcon {

	/**
	 * @return the small version of the icon
	 */
	ImageIcon small();

	/**
	 * @return the large version of the icon
	 */
	ImageIcon large();

	/**
	 * Creates a new {@link ControlIcon} using the same icon for both small and large.
	 * @param icon the icon, used as both small and large
	 * @return a new {@link ControlIcon} instance
	 */
	static ControlIcon controlIcon(ImageIcon icon) {
		return new DefaultControlIcon(icon, icon);
	}

	/**
	 * @param small the small version of the icon
	 * @param large the large version of the icon
	 * @return a new {@link ControlIcon} instance
	 */
	static ControlIcon controlIcon(ImageIcon small, ImageIcon large) {
		return new DefaultControlIcon(small, large);
	}
}
