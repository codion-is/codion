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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.reactive.value.Value;

import org.jspecify.annotations.Nullable;

import javax.swing.KeyStroke;

import static java.util.Objects.requireNonNull;

final class DefaultControlKey<T extends Control> implements ControlKey<T> {

	private final String name;
	private final Value<KeyStroke> defaultKeyStroke;

	DefaultControlKey(String name, @Nullable KeyStroke defaultKeyStroke) {
		this.name = requireNonNull(name);
		this.defaultKeyStroke = Value.nullable(defaultKeyStroke);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Value<KeyStroke> defaultKeystroke() {
		return defaultKeyStroke;
	}

	@Override
	public String toString() {
		return name;
	}
}
