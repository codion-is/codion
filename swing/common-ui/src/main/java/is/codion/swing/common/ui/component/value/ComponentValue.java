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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.value;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.Components;

import javax.swing.JComponent;

/**
 * A {@link Value} represented by an input component of some sort.
 * <p>
 * {@link Components} is a factory for {@link ComponentValue} implementations.
 * @param <C> the component type
 * @param <T> the value type
 */
public interface ComponentValue<C extends JComponent, T> extends Value<T> {

	/**
	 * @return the input component representing the value
	 */
	C component();
}
