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

import static java.util.Objects.requireNonNull;

final class DefaultControlIcon implements ControlIcon {

	private final ImageIcon small;
	private final ImageIcon large;

	DefaultControlIcon(ImageIcon small, ImageIcon large) {
		this.small = requireNonNull(small);
		this.large = requireNonNull(large);
	}

	@Override
	public ImageIcon small() {
		return small;
	}

	@Override
	public ImageIcon large() {
		return large;
	}
}
