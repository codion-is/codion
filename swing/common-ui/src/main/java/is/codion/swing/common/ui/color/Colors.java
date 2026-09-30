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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.color;

import java.awt.Color;

import static java.util.Objects.requireNonNull;

/**
 * Utilities class for Color.
 */
public final class Colors {

	private Colors() {}

	/**
	 * Returns a darker version of the given color, using 0.8 as the mulitiplication factor.
	 * @param color the color to darken
	 * @return a darker version of the given color
	 * @see Color#darker()
	 */
	public static Color darker(Color color) {
		return darker(color, 0.8);
	}

	/**
	 * Returns a darker version of the given color, using the given factor.
	 * @param color the color to darken
	 * @param factor a number between 0 and 1, non-inclusive
	 * @return a darker version of the given color
	 * @see Color#darker()
	 */
	public static Color darker(Color color, double factor) {
		requireNonNull(color);
		if (factor <= 0 || factor >= 1) {
			throw new IllegalArgumentException("Factor must be between 0 and 1, non-inclusive");
		}

		return new Color(Math.max((int) (color.getRed() * factor), 0),
						Math.max((int) (color.getGreen() * factor), 0),
						Math.max((int) (color.getBlue() * factor), 0),
						color.getAlpha());
	}

	/**
	 * Returns a shade of the given color, using 0.06 as the fraction.
	 * @param color the color to shade
	 * @return a shade of the given color
	 * @see #shade(Color, double)
	 */
	public static Color shade(Color color) {
		return shade(color, 0.06);
	}

	/**
	 * Returns a shade of the given color, the given fraction of the way to black, or to white in case of a dark color,
	 * darkness decided by perceived luminance. Unlike {@link #darker(Color, double)}, the difference is visible
	 * on dark colors as well as light ones, making this suitable for shading backgrounds in both light and dark
	 * look and feels.
	 * @param color the color to shade
	 * @param fraction a number between 0 and 1, non-inclusive
	 * @return a shade of the given color
	 */
	public static Color shade(Color color, double fraction) {
		requireNonNull(color);
		if (fraction <= 0 || fraction >= 1) {
			throw new IllegalArgumentException("Fraction must be between 0 and 1, non-inclusive");
		}
		int target = dark(color) ? 255 : 0;

		return new Color(shade(color.getRed(), target, fraction),
						shade(color.getGreen(), target, fraction),
						shade(color.getBlue(), target, fraction),
						color.getAlpha());
	}

	private static int shade(int component, int target, double fraction) {
		return (int) Math.round(component + (target - component) * fraction);
	}

	// by perceived luminance, ITU-R BT.601
	private static boolean dark(Color color) {
		return 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue() < 128;
	}
}
