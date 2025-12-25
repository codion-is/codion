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

import javax.swing.ImageIcon;
import java.awt.Color;
import java.net.URL;

import static java.util.Objects.requireNonNull;

/**
 * A SVG based icon.
 */
public interface SVGIcon {

	/**
	 * @return the image icon
	 */
	ImageIcon imageIcon();

	/**
	 * @return the size
	 */
	int size();

	/**
	 * Sets the icon color
	 * @param color the color
	 */
	void color(Color color);

	/**
	 * Creates a derived copy of this icon using given size
	 * @param size the size
	 * @return a new icon
	 */
	SVGIcon derive(int size);

	/**
	 * Instantiates a new {@link SVGIcon}
	 * @param svgIconUrl the svg icon resource url
	 * @param size the size
	 * @param color the color
	 * @return a new {@link SVGIcon}
	 */
	static SVGIcon icon(URL svgIconUrl, int size, Color color) {
		return new DefaultSVGIcon(requireNonNull(svgIconUrl, "SVG icon URL is null"), size, requireNonNull(color));
	}
}
