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
package is.codion.swing.common.ui.icon;

import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.Configuration;
import is.codion.common.utilities.property.PropertyValue;

import java.awt.Color;
import java.net.URL;

import static javax.swing.UIManager.getColor;

/**
 * Provides icons for ui components.
 * {@link #color()} follows the 'Button.foreground' color of the current Look and feel.
 * Add icons via {@link #put(String, URL)} and retrieve them via {@link #get(String)}.
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
	 * Adds the given icon to this Icons instance, using {@link #size()} and {@link #color()}.
	 * Retrieve an icon via {@link #get(String)}.
	 * @param identifier the icon identifier
	 * @param svgUrl the svg resource url
	 * @throws IllegalArgumentException in case an icon has already been associated with the given identifier
	 */
	void put(String identifier, URL svgUrl);

	/**
	 * Adds the given icon to this SVGIcons instance.
	 * Retrieve an icon via {@link #get(String)}.
	 * @param identifier the icon identifier
	 * @param icon the icon
	 * @throws IllegalArgumentException in case an icon has already been associated with the given identifier
	 */
	void put(String identifier, SVGIcon icon);

	/**
	 * Retrieves the icon associated with the given ikon from this Icons instance.
	 * @param identifier the icon identifier
	 * @return the {@link SVGIcon} associated with the given identifier
	 * @throws IllegalArgumentException in case no icon has been associated with the given identifier
	 */
	SVGIcon get(String identifier);

	/**
	 * @param size the icon size
	 * @return a new {@link Icons} instance
	 */
	static Icons icons(int size) {
		return new DefaultIcons(size);
	}
}
