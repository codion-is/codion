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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.spinner;

/**
 * A builder for JSpinner based on a list of {@link is.codion.common.utilities.item.Item}s.
 */
public interface ItemSpinnerBuilder<T> extends SpinnerBuilder<T, ItemSpinnerBuilder<T>> {

	/**
	 * @param <T> the value type
	 * @return a builder for a JSpinner
	 */
	static <T> ItemSpinnerBuilder<T> builder() {
		return new DefaultItemSpinnerBuilder<>();
	}
}
