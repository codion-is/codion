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
 * <p>Provides a validation indicator for a component, over the two severities validation has: a value that is
 * <em>invalid</em> and may not be saved, and one that is merely <em>warned</em> about — permitted, but implausible
 * enough to be worth a second look.
 * <p>Both severities go through one indicator rather than two, because they share a visual channel and the precedence
 * between them (invalid wins) has to be decided in one place.
 */
public interface ValidationIndicator {

	/**
	 * Specified the {@link ValidationIndicator} to use.
	 * <p>Default {@link BackgroundColorValidationIndicator}
	 */
	PropertyValue<String> INDICATOR_CLASS =
					stringValue(ValidationIndicator.class.getName() + ".implementation", BackgroundColorValidationIndicator.class.getName());

	/**
	 * Enables the validation indicator for the given component
	 * @param component the component
	 * @param valid the valid state observer, false while the value may not be saved
	 * @param warned the warned state observer, true while the value carries a warning. Independent of [valid] — a value
	 * can be both, in which case the invalid presentation wins
	 */
	void enable(JComponent component, ObservableState valid, ObservableState warned);

	/**
	 * Returns an instance from the {@link ServiceLoader}, of the type specified by {@link #INDICATOR_CLASS}
	 * @return an instance from the {@link ServiceLoader} or an empty {@link Optional} in case one is not found
	 */
	static Optional<ValidationIndicator> instance() {
		return instance(INDICATOR_CLASS.getOrThrow());
	}

	/**
	 * Returns an instance from the {@link ServiceLoader}, of the type specified by {@code indicatorClassName}
	 * @return an instance from the {@link ServiceLoader} or an empty {@link Optional} in case one is not found
	 */
	static Optional<ValidationIndicator> instance(String indicatorClassName) {
		requireNonNull(indicatorClassName);

		return stream(ServiceLoader.load(ValidationIndicator.class).spliterator(), false)
						.filter(factory -> factory.getClass().getName().equals(indicatorClassName))
						.findFirst();
	}
}
