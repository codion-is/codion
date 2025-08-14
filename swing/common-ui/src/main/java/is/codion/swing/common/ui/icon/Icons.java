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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.icon;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;

import org.kordamp.ikonli.Ikon;

import javax.swing.ImageIcon;
import java.awt.Color;

import static is.codion.common.Configuration.integerValue;
import static javax.swing.UIManager.getColor;

/**
 * Provides icons for ui components.
 * {@link #COLOR} follows the 'Button.foreground' color of the current Look and feel.
 * Add icons via {@link #add(Ikon...)} and retrieve them via {@link #get(Ikon)}.
 * @see #icons()
 */
public interface Icons {

	/**
	 * The icon size, note that this will affect the size of buttons
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 16
	 * </ul>
	 */
	PropertyValue<Integer> SIZE = integerValue(Icons.class.getName() + ".size", 16);

	/**
	 * The icon color, follows the "Button.foreground" color of the current Look and feel.
	 * <ul>
	 * <li>Value type: Color
	 * <li>Default value: UIManager.getColor("Button.foreground")
	 * </ul>
	 */
	PropertyValue<Color> COLOR = Configuration.value(Icons.class.getName() + ".color", Color::decode, getColor("Button.foreground"));

	/**
	 * Adds the given ikons to this FrameworkIcons instance. Retrieve an icon via {@link #get(Ikon)}.
	 * @param ikons the ikons to add
	 * @throws IllegalArgumentException in case an icon has already been associated with any of the given ikons
	 */
	void add(Ikon... ikons);

	/**
	 * Retrieves the ImageIcon associated with the given ikon from this FrameworkIcons instance.
	 * @param ikon the ikon
	 * @return the ImageIcon associated with the given ikon
	 * @throws IllegalArgumentException in case no icon has been associated with the given ikon
	 * @see #add(Ikon...)
	 */
	ImageIcon get(Ikon ikon);

	/**
	 * @return a new {@link Icons} instance
	 */
	static Icons icons() {
		return new DefaultIcons();
	}
}
