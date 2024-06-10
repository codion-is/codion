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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.value.Value;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.*;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class DefaultControlMap implements ControlMap {

	private final Map<ControlKey<?>, Value<Control>> controls;

	DefaultControlMap(Class<?> controlKeysClass) {
		this(Stream.of(controlKeysClass.getFields())
						.filter(DefaultControlMap::publicStaticFinalControlKey)
						.map(DefaultControlMap::controlKey)
						.collect(toList()));
	}

	private DefaultControlMap(Collection<ControlKey<?>> controlKeys) {
		controls = unmodifiableMap(controlKeys.stream()
						.collect(toMap(Function.identity(), controlKey -> Value.<Control>nullable().build())));
	}

	@Override
	public <T extends Control> Value<T> control(ControlKey<T> controlKey) {
		Value<Control> value = controls.get(requireNonNull(controlKey));
		if (value == null) {
			throw new IllegalArgumentException("Unknown controlKey");
		}

		return (Value<T>) value;
	}

	@Override
	public Collection<Value<Control>> controls() {
		return controls.values();
	}

	static boolean publicStaticFinalControlKey(Field field) {
		return isPublic(field.getModifiers())
						&& isStatic(field.getModifiers())
						&& isFinal(field.getModifiers())
						&& ControlKey.class.isAssignableFrom(field.getType());
	}

	static ControlKey<?> controlKey(Field field) {
		try {
			return (ControlKey<?>) field.get(null);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
