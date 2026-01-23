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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.indicator;

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.utilities.property.PropertyValue;

import javax.swing.JComponent;
import java.util.Optional;
import java.util.ServiceLoader;

import static is.codion.common.utilities.Configuration.stringValue;
import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

/**
 * Provides a validity indicator for a component.
 */
public interface ValidIndicator {

	/**
	 * Specified the {@link ValidIndicator} to use.
	 * <p>Default {@link BackgroundColorValidIndicator}
	 */
	PropertyValue<String> INDICATOR_CLASS =
					stringValue(ValidIndicator.class.getName() + ".indicatorClass", BackgroundColorValidIndicator.class.getName());

	/**
	 * Enables the valid indicator for the given component, based on the given valid state
	 * @param component the component
	 * @param valid the valid state observer
	 */
	void enable(JComponent component, ObservableState valid);

	/**
	 * Returns an instance from the {@link ServiceLoader}, of the type specified by {@link #INDICATOR_CLASS}
	 * @return an instance from the {@link ServiceLoader} or an empty {@link Optional} in case one is not found
	 */
	static Optional<ValidIndicator> instance() {
		return instance(INDICATOR_CLASS.getOrThrow());
	}

	/**
	 * Returns an instance from the {@link ServiceLoader}, of the type specified by {@code indicatorClassName}
	 * @return an instance from the {@link ServiceLoader} or an empty {@link Optional} in case one is not found
	 */
	static Optional<ValidIndicator> instance(String indicatorClassName) {
		requireNonNull(indicatorClassName);

		return stream(ServiceLoader.load(ValidIndicator.class).spliterator(), false)
						.filter(factory -> factory.getClass().getName().equals(indicatorClassName))
						.findFirst();
	}
}
