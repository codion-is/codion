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

import javax.swing.KeyStroke;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

final class DefaultControlKeyStrokes implements ControlKeyStrokes {

	private final Map<ControlKey<?>, Value<KeyStroke>> keyStrokes = new HashMap<>();

	DefaultControlKeyStrokes(Class<?> controlKeysClass) {
		this(Stream.of(controlKeysClass.getFields())
						.filter(DefaultControlMap::publicStaticFinalControlKey)
						.map(DefaultControlMap::controlKey)
						.collect(toList()));
	}

	DefaultControlKeyStrokes(Collection<ControlKey<?>> controlKeys) {
		controlKeys.forEach(controlKey ->
						keyStrokes.put(controlKey, Value.nullable(controlKey.defaultKeystroke().get()).build()));
	}

	private DefaultControlKeyStrokes(DefaultControlKeyStrokes controlKeyStrokes) {
		controlKeyStrokes.keyStrokes.forEach((controlKey, keyStrokeValue) ->
						keyStrokes.put(controlKey, Value.nullable(keyStrokeValue.get()).build()));
	}

	@Override
	public Value<KeyStroke> keyStroke(ControlKey<?> controlKey) {
		Value<KeyStroke> keyStroke = keyStrokes.get(controlKey);
		if (keyStroke == null) {
			throw new IllegalArgumentException("Unknown controlKey");
		}

		return keyStroke;
	}

	@Override
	public ControlKeyStrokes copy() {
		return new DefaultControlKeyStrokes(this);
	}
}
