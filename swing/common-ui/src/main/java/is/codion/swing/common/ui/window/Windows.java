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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.window;

import org.jspecify.annotations.Nullable;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Window;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for windows.
 */
public final class Windows {

	private Windows() {}

	/**
	 * @param ratio a ratio, 0.0 - 1.0
	 * @return a Dimension which is the size of the available screen times ratio
	 * @throws IllegalArgumentException in case ratio is not between 0 and 1
	 */
	public static Dimension screenSizeRatio(double ratio) {
		if (ratio < 0 || ratio > 1.0) {
			throw new IllegalArgumentException("Ratio must be between 0 and 1");
		}
		Dimension screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();

		return new Dimension((int) (screen.getWidth() * ratio), (int) (screen.getHeight() * ratio));
	}

	/**
	 * Resizes the given window so that it fits within the usable screen bounds,
	 * excluding taskbars, panels and other desktop decorations.
	 * <p>If the window already fits then calling this method has no effect.
	 * @param window the window to resize
	 */
	public static void resizeToFitScreen(Window window) {
		requireNonNull(window);
		Dimension usableSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
		Dimension windowSize = window.getSize();
		if (windowSize.getHeight() > usableSize.getHeight() || windowSize.getWidth() > usableSize.getWidth()) {
			window.setSize(new Dimension((int) Math.min(windowSize.getWidth(), usableSize.getWidth()),
							(int) Math.min(windowSize.getHeight(), usableSize.getHeight())));
		}
	}

	/**
	 * Resizes the given window so that it is {@code screenSizeRatio} percent of the current screen size
	 * @param window the window to resize
	 * @param screenSizeRatio the screen size ratio
	 * @throws IllegalArgumentException in case ratio is not between 0 and 1
	 */
	public static void resize(Window window, double screenSizeRatio) {
		resize(window, screenSizeRatio, null, null);
	}

	/**
	 * Resizes the given window so that it is {@code screenSizeRatio} percent of the current screen size,
	 * within the given minimum and maximum sizes
	 * @param window the window to resize
	 * @param screenSizeRatio the screen size ratio
	 * @param minimumSize the minimum size, may be null
	 * @param maximumSize the maximum size, may be null
	 * @throws IllegalArgumentException in case ratio is not between 0 and 1
	 */
	public static void resize(Window window, double screenSizeRatio, @Nullable Dimension minimumSize, @Nullable Dimension maximumSize) {
		Dimension ratioSize = screenSizeRatio(screenSizeRatio);
		if (minimumSize != null) {
			ratioSize.setSize(Math.max(minimumSize.width, ratioSize.width), Math.max(minimumSize.height, ratioSize.height));
		}
		if (maximumSize != null) {
			ratioSize.setSize(Math.min(maximumSize.width, ratioSize.width), Math.min(maximumSize.height, ratioSize.height));
		}

		window.setSize(ratioSize);
	}
}
