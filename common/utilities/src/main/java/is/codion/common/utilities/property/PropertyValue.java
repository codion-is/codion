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
package is.codion.common.utilities.property;

import is.codion.common.value.AbstractValue;

import org.jspecify.annotations.Nullable;

import java.util.Properties;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A Value associated with a named property.
 * @param <T> the value type
 */
public final class PropertyValue<T> extends AbstractValue<T> {

	private final Properties properties;
	private final String name;
	private final Function<T, String> encoder;

	private @Nullable T value;

	PropertyValue(Properties properties, String name, Function<String, T> decoder, Function<T, String> encoder, @Nullable T defaultValue) {
		super(defaultValue, Notify.CHANGED);
		this.properties = properties;
		this.name = requireNonNull(name);
		this.encoder = requireNonNull(encoder);
		set(getInitialValue(name, requireNonNull(decoder)));
	}

	/**
	 * @return the name of the property this value represents
	 */
	public String name() {
		return name;
	}

	@Override
	public T getOrThrow() {
		return getOrThrow("Required configuration value is missing: " + name);
	}

	/**
	 * Sets this value to null as well as removing it from the underlying store and clearing the system property.
	 */
	public void remove() {
		boolean wasNotNull = value != null;
		setValue(null);
		if (wasNotNull) {
			notifyObserver();
		}
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	protected @Nullable T getValue() {
		return value;
	}

	@Override
	protected void setValue(@Nullable T value) {
		this.value = value;
		if (value == null) {
			properties.remove(name);
			System.clearProperty(name);
		}
		else {
			properties.setProperty(name, encoder.apply(value));
			System.setProperty(name, properties.getProperty(name));
		}
	}

	private @Nullable T getInitialValue(String property, Function<String, T> decoder) {
		String initialValue = System.getProperty(property);
		if (initialValue == null) {
			initialValue = properties.getProperty(property);
		}

		return initialValue == null ? null : decoder.apply(initialValue);
	}
}
