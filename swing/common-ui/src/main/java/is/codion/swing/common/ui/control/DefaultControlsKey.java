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
import is.codion.swing.common.ui.control.Controls.ControlsKey;
import is.codion.swing.common.ui.control.Controls.Layout;

import javax.swing.KeyStroke;
import java.util.Optional;

final class DefaultControlsKey implements ControlsKey {

	private final Value<KeyStroke> defaultKeyStroke;
	private final Layout defaultLayout;

	DefaultControlsKey(KeyStroke defaultKeyStroke, Layout defaultLayout) {
		this.defaultKeyStroke = Value.nullable(defaultKeyStroke).build();
		this.defaultLayout = defaultLayout;
	}

	@Override
	public Class<Controls> controlClass() {
		return Controls.class;
	}

	@Override
	public Value<KeyStroke> defaultKeystroke() {
		return defaultKeyStroke;
	}

	@Override
	public Optional<Layout> defaultLayout() {
		return Optional.ofNullable(defaultLayout);
	}
}
