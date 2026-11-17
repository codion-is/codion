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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.image;

/**
 * Fits an image to an area, its proportions kept, for {@link ImagePane} and {@link ImageView}.
 */
final class ImageFit {

	private ImageFit() {}

	/**
	 * @param imageWidth the width of the image
	 * @param imageHeight the height of the image
	 * @param width the width of the area
	 * @param height the height of the area
	 * @param enlarge true if an image smaller than the area should be enlarged to fit it
	 * @return the scale fitting the image to the area, its proportions kept, at most 1 unless enlarging
	 */
	static double scale(int imageWidth, int imageHeight, int width, int height, boolean enlarge) {
		double scale = Math.min((double) width / imageWidth, (double) height / imageHeight);

		return enlarge ? scale : Math.min(scale, 1);
	}
}
