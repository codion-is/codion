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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.reactive.state.ObservableState;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.function.Supplier;

/**
 * A builder for Control
 * @param <C> the Control type
 * @param <B> the builder type
 */
public interface ControlBuilder<C extends Control, B extends ControlBuilder<C, B>> extends Supplier<C> {

	/**
	 * @param caption the caption for the control
	 * @return this Builder instance
	 */
	B caption(@Nullable String caption);

	/**
	 * @param enabled the state observer which controls the enabled state of the control
	 * @return this Builder instance
	 */
	B enabled(@Nullable ObservableState enabled);

	/**
	 * @param mnemonic the control mnemonic
	 * @return this Builder instance
	 */
	B mnemonic(int mnemonic);

	/**
	 * Sets both the small and large icons.
	 * @param icon the control icon
	 * @return this Builder instance
	 * @see #smallIcon(Icon)
	 * @see #largeIcon(Icon)
	 */
	B icon(@Nullable ControlIcon icon);

	/**
	 * @param smallIcon the small control icon
	 * @return this Builder instance
	 */
	B smallIcon(@Nullable Icon smallIcon);

	/**
	 * @param largeIcon the large control icon
	 * @return this Builder instance
	 */
	B largeIcon(@Nullable Icon largeIcon);

	/**
	 * @param description a string describing the control
	 * @return this Builder instance
	 */
	B description(@Nullable String description);

	/**
	 * @param foreground the foreground color
	 * @return this Builder instance
	 */
	B foreground(@Nullable Color foreground);

	/**
	 * @param background the background color
	 * @return this Builder instance
	 */
	B background(@Nullable Color background);

	/**
	 * @param font the font
	 * @return this Builder instance
	 */
	B font(@Nullable Font font);

	/**
	 * @param keyStroke the keystroke to associate with the control
	 * @return this Builder instance
	 */
	B keyStroke(@Nullable KeyStroke keyStroke);

	/**
	 * Note that any values added will overwrite the property, if already present,
	 * i.e. setting the 'SmallIcon' value via this method will overwrite the one set
	 * via {@link #smallIcon(Icon)}.
	 * @param key the key
	 * @param value the value
	 * @return this builder
	 * @see Action#putValue(String, Object)
	 */
	B value(String key, @Nullable Object value);

	/**
	 * @return a new Control instance
	 */
	C build();

	@Override
	default C get() {
		return build();
	}
}
