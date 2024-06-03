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

final class DefaultControlShortcuts implements ControlShortcuts {

	private final Map<ControlId<?>, Value<KeyStroke>> keyStrokes = new HashMap<>();

	DefaultControlShortcuts(Class<?> controlIdsClass) {
		this(Stream.of(controlIdsClass.getFields())
						.filter(DefaultControlSet::publicStaticFinalControlId)
						.map(DefaultControlSet::controlId)
						.collect(toList()));
	}

	DefaultControlShortcuts(Collection<ControlId<?>> controlIds) {
		controlIds.forEach(controlId -> keyStrokes.put(controlId, Value.<KeyStroke>nullable()
						.initialValue(controlId.defaultKeystroke().orElse(null))
						.build()));
	}

	private DefaultControlShortcuts(DefaultControlShortcuts controlKeyStrokes) {
		controlKeyStrokes.keyStrokes.forEach((controlType, keyStrokeValue) ->
						keyStrokes.put(controlType, Value.<KeyStroke>nullable()
										.initialValue(keyStrokeValue.get())
										.build()));
	}

	@Override
	public Value<KeyStroke> keyStroke(ControlId<?> controlId) {
		Value<KeyStroke> keyStroke = keyStrokes.get(controlId);
		if (keyStroke == null) {
			throw new IllegalArgumentException("Unknown controlId");
		}

		return keyStroke;
	}

	@Override
	public ControlShortcuts copy() {
		return new DefaultControlShortcuts(this);
	}
}
