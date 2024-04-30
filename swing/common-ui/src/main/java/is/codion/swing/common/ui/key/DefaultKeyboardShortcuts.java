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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.key;

import is.codion.common.value.Value;

import javax.swing.KeyStroke;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

final class DefaultKeyboardShortcuts<T extends Enum<T> & KeyboardShortcuts.Shortcut> implements KeyboardShortcuts<T> {

	private final Class<T> shortcutClass;
	private final Map<T, Value<KeyStroke>> keyStrokes;

	DefaultKeyboardShortcuts(Class<T> shortcutClass) {
		this(requireNonNull(shortcutClass), Stream.of(shortcutClass.getEnumConstants())
						.collect(toMap(Function.identity(), DefaultKeyboardShortcuts::keyStrokeValue)));
	}

	private DefaultKeyboardShortcuts(Class<T> shortcutClass, Map<T, Value<KeyStroke>> keyStrokes) {
		this.shortcutClass = shortcutClass;
		this.keyStrokes = keyStrokes;
	}

	@Override
	public Value<KeyStroke> keyStroke(T shortcut) {
		return keyStrokes.get(requireNonNull(shortcut));
	}

	@Override
	public KeyboardShortcuts<T> copy() {
		return new DefaultKeyboardShortcuts<>(shortcutClass, keyStrokes.entrySet().stream()
						.collect(toMap(Map.Entry::getKey, entry ->
										Value.nullable(entry.getValue().get()).build())));
	}

	private static <T extends Enum<T> & Shortcut> Value<KeyStroke> keyStrokeValue(T shortcutKey) {
		Value<KeyStroke> value = Value.<KeyStroke>nullable().build();
		shortcutKey.defaultKeystroke().ifPresent(value::set);

		return value;
	}
}
