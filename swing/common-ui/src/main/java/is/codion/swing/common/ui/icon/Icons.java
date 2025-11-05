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

import is.codion.common.utilities.Configuration;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.value.Value;

import org.kordamp.ikonli.Ikon;

import javax.swing.ImageIcon;
import java.awt.Color;

import static javax.swing.UIManager.getColor;

/**
 * Provides icons for ui components.
 * {@link #color()} follows the 'Button.foreground' color of the current Look and feel.
 * Add icons via {@link #add(Ikon...)} and retrieve them via {@link #get(Ikon)}.
 * @see #icons(int)
 */
public interface Icons {

	/**
	 * The default icon color.
	 * <ul>
	 * <li>Value type: Color
	 * <li>Default value: UIManager.getColor("Button.foreground")
	 * </ul>
	 */
	PropertyValue<Color> COLOR = Configuration.value(Icons.class.getName() + ".color", Color::decode, getColor("Button.foreground"));

	/**
	 * Follows the 'Button.foreground' color of the current Look and feel.
	 * @return the {@link Value} controlling the icon color
	 */
	Value<Color> color();

	/**
	 * @return the icon size
	 */
	int size();

	/**
	 * Adds the given ikons to this Icons instance. Retrieve an icon via {@link #get(Ikon)}.
	 * @param ikons the ikons to add
	 * @throws IllegalArgumentException in case an icon has already been associated with any of the given ikons
	 */
	void add(Ikon... ikons);

	/**
	 * Retrieves the ImageIcon associated with the given ikon from this Icons instance.
	 * @param ikon the ikon
	 * @return the ImageIcon associated with the given ikon
	 * @throws IllegalArgumentException in case no icon has been associated with the given ikon
	 * @see #add(Ikon...)
	 */
	ImageIcon get(Ikon ikon);

	/**
	 * @param size the icon size
	 * @return a new {@link Icons} instance
	 */
	static Icons icons(int size) {
		return new DefaultIcons(size);
	}
}
