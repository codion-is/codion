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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.indicator;

import is.codion.common.property.PropertyValue;
import is.codion.common.state.ObservableState;

import javax.swing.JComponent;
import java.util.Optional;
import java.util.ServiceLoader;

import static is.codion.common.Configuration.stringValue;
import static java.util.stream.StreamSupport.stream;

/**
 * Provides a validity indicator for a component.
 */
public interface ValidIndicatorFactory {

	/**
	 * Specified the {@link ValidIndicatorFactory} to use
	 */
	PropertyValue<String> FACTORY_CLASS =
					stringValue(ValidIndicatorFactory.class.getName() + ".factoryClass", DefaultValidIndicatorFactory.class.getName());

	/**
	 * Enables the valid indicator for the given component, based on the given valid state
	 * @param component the component
	 * @param valid the valid state observer
	 */
	void enable(JComponent component, ObservableState valid);

	/**
	 * Returns an instance from the {@link ServiceLoader}, of the type specified by {@link #FACTORY_CLASS}
	 * @return an instance from the {@link ServiceLoader} or an empty {@link Optional} in case one is not found
	 */
	static Optional<ValidIndicatorFactory> instance() {
		String classname = FACTORY_CLASS.getOrThrow();

		return stream(ServiceLoader.load(ValidIndicatorFactory.class).spliterator(), false)
						.filter(factory -> factory.getClass().getName().equals(classname))
						.findFirst();
	}
}
