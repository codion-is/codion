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

final class DefaultControlSet implements ControlSet {

	private final Map<ControlId<?>, Value<Control>> controls;

	DefaultControlSet(Class<?> controlIdsClass) {
		this(Stream.of(controlIdsClass.getFields())
						.filter(DefaultControlSet::publicStaticFinalControlId)
						.map(DefaultControlSet::controlId)
						.collect(toList()));
	}

	private DefaultControlSet(Collection<ControlId<?>> controlIds) {
		controls = unmodifiableMap(controlIds.stream()
						.collect(toMap(Function.identity(), controlId -> Value.<Control>nullable().build())));
	}

	@Override
	public <T extends Control> Value<T> control(ControlId<T> controlId) {
		Value<Control> value = controls.get(requireNonNull(controlId));
		if (value == null) {
			throw new IllegalArgumentException("Unknown controlId");
		}

		return (Value<T>) value;
	}

	@Override
	public Collection<Value<Control>> controls() {
		return controls.values();
	}

	static boolean publicStaticFinalControlId(Field field) {
		return isPublic(field.getModifiers())
						&& isStatic(field.getModifiers())
						&& isFinal(field.getModifiers())
						&& ControlId.class.isAssignableFrom(field.getType());
	}

	static ControlId<?> controlId(Field field) {
		try {
			return (ControlId<?>) field.get(null);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
