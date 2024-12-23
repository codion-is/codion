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
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.KeyStroke;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class DefaultControlMap implements ControlMap {

	private final Map<ControlKey<?>, Value<Control>> controls = new HashMap<>();
	private final Map<ControlKey<?>, Value<KeyStroke>> keyStrokes = new HashMap<>();

	DefaultControlMap(Class<?> controlKeysClass) {
		this(Stream.of(controlKeysClass.getFields())
						.filter(DefaultControlMap::publicStaticFinalControlKey)
						.map(DefaultControlMap::controlKey)
						.collect(toList()));
	}

	private DefaultControlMap(Collection<ControlKey<?>> controlKeys) {
		controls.putAll(controlKeys.stream()
						.collect(toMap(Function.identity(), controlKey -> Value.nullable())));
		keyStrokes.putAll(controlKeys.stream()
						.collect(toMap(Function.identity(), controlKey -> Value.nullable(controlKey.defaultKeystroke().get()))));
	}

	private DefaultControlMap(DefaultControlMap controlMap) {
		controlMap.controls.forEach((controlKey, controlValue) ->
						controls.put(controlKey, Value.nullable(controlValue.get())));
		controlMap.keyStrokes.forEach((controlKey, keyStrokeValue) ->
						keyStrokes.put(controlKey, Value.nullable(keyStrokeValue.get())));
	}

	@Override
	public <T extends Control> Value<T> control(ControlKey<T> controlKey) {
		Value<Control> value = controls.get(requireNonNull(controlKey));
		if (value == null) {
			throw new IllegalArgumentException("Unknown controlKey: " + controlKey);
		}

		return (Value<T>) value;
	}

	@Override
	public Collection<Value<Control>> controls() {
		return controls.values();
	}

	@Override
	public Value<KeyStroke> keyStroke(ControlKey<?> controlKey) {
		Value<KeyStroke> keyStroke = keyStrokes.get(requireNonNull(controlKey));
		if (keyStroke == null) {
			throw new IllegalArgumentException("Unknown controlKey: " + controlKey);
		}

		return keyStroke;
	}

	@Override
	public Optional<KeyEvents.Builder> keyEvent(ControlKey<?> controlKey) {
		Optional<KeyStroke> keyStroke = keyStroke(controlKey).optional();
		Optional<? extends Control> control = control(controlKey).optional();
		if (keyStroke.isPresent() && control.isPresent()) {
			return Optional.of(KeyEvents.builder(keyStroke.get())
							.action(control.get()));
		}

		return Optional.empty();
	}

	@Override
	public ControlMap copy() {
		return new DefaultControlMap(this);
	}

	private static boolean publicStaticFinalControlKey(Field field) {
		return isPublic(field.getModifiers())
						&& isStatic(field.getModifiers())
						&& isFinal(field.getModifiers())
						&& ControlKey.class.isAssignableFrom(field.getType());
	}

	private static ControlKey<?> controlKey(Field field) {
		try {
			return (ControlKey<?>) field.get(null);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
