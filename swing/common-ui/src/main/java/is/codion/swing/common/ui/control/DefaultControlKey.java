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

import static java.util.Objects.requireNonNull;

final class DefaultControlKey<T extends Control> implements ControlKey<T> {

	private final Class<T> controlClass;
	private final Value<KeyStroke> defaultKeyStroke;

	DefaultControlKey(Class<T> controlClass, KeyStroke defaultKeyStroke) {
		this.controlClass = requireNonNull(controlClass);
		this.defaultKeyStroke = Value.nullable(defaultKeyStroke).build();
	}

	@Override
	public Class<T> controlClass() {
		return controlClass;
	}

	@Override
	public Value<KeyStroke> defaultKeystroke() {
		return defaultKeyStroke;
	}
}
