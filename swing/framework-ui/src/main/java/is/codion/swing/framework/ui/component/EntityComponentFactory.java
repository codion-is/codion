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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;

import javax.swing.JComponent;

/**
 * A factory for {@link ComponentValue} implementations.
 * @param <T> the value type
 * @param <A> the attribute type
 * @param <C> the component type
 */
public interface EntityComponentFactory<T, A extends Attribute<T>, C extends JComponent> {

	/**
	 * Provides value input components for multiple entity update, override to supply
	 * specific {@link ComponentValue} implementations for attributes.
	 * @param attribute the attribute for which to get the ComponentValue
	 * @param editModel the edit model used to create foreign key input models
	 * @param initialValue the initial value to display
	 * @return a new ComponentValue instance handling input for {@code attribute}
	 */
	ComponentValue<T, C> componentValue(A attribute, SwingEntityEditModel editModel, T initialValue);
}
