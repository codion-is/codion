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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;

import javax.swing.JComponent;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A factory for {@link ComponentValue} instances.
 * @param <T> the value type
 * @param <C> the component type
 */
public interface EditComponentFactory<T, C extends JComponent> {

	/**
	 * Provides an input {@link ComponentValue} for editing a single attribute value for one or more entities.
	 * @param editModel the edit model used to create foreign key input models
	 * @return a new {@link ComponentValue} instance
	 */
	ComponentValue<T, C> component(SwingEntityEditModel editModel);

	/**
	 * Provides a way to override the default attribute caption, when presenting the component to the user.
	 * @param attributeDefinition the attribute definition
	 * @return a caption to use when displaying the component, or an empty {@link Optional} for no caption
	 */
	default Optional<String> caption(AttributeDefinition<T> attributeDefinition) {
		return Optional.of(requireNonNull(attributeDefinition).caption());
	}
}
