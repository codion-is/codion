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
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.util.HashMap;
import java.util.Map;

abstract class AbstractControlBuilder<C extends Control, B extends Control.Builder<C, B>> implements Control.Builder<C, B> {

	private final Map<String, Object> values = new HashMap<>();

	protected String name;
	protected StateObserver enabled;
	private char mnemonic;
	private Icon smallIcon;
	private Icon largeIcon;
	private String description;
	private KeyStroke keyStroke;

	@Override
	public final B name(String name) {
		this.name = name;
		return (B) this;
	}

	@Override
	public final B enabled(StateObserver enabled) {
		this.enabled = enabled;
		return (B) this;
	}

	@Override
	public final B mnemonic(char mnemonic) {
		this.mnemonic = mnemonic;
		return (B) this;
	}

	@Override
	public final B smallIcon(Icon smallIcon) {
		this.smallIcon = smallIcon;
		return (B) this;
	}

	@Override
	public final B largeIcon(Icon largeIcon) {
		this.largeIcon = largeIcon;
		return (B) this;
	}

	@Override
	public final B description(String description) {
		this.description = description;
		return (B) this;
	}

	@Override
	public final B keyStroke(KeyStroke keyStroke) {
		this.keyStroke = keyStroke;
		return (B) this;
	}

	@Override
	public final B value(String key, Object value) {
		if ("enabled".equals(key)) {
			throw new IllegalArgumentException("Can not set the enabled property of a Control");
		}
		values.put(key, value);
		return (B) this;
	}

	@Override
	public C build() {
		C control = createControl();
		control.setMnemonic(mnemonic);
		control.setSmallIcon(smallIcon);
		control.setLargeIcon(largeIcon);
		control.setDescription(description);
		control.setKeyStroke(keyStroke);
		values.forEach(control::putValue);

		return control;
	}

	protected abstract C createControl();
}
